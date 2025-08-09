#!/bin/bash
# GitHub 仓库信息
REPO="Aqr-K/forward-panel"
# 安装目录
INSTALL_DIR="/etc/gost"

# 显示菜单
show_menu() {
  echo "==============================================="
  echo "              GOST 节点管理脚本"
  echo "==============================================="
  echo "请选择操作："
  echo "1. 安装/更新 (最新稳定版)"
  echo "2. 安装/更新 (预发布版)"
  echo "3. 卸载 GOST"
  echo "4. 退出"
  echo "==============================================="
}

# 自动检测系统架构
get_arch() {
  case $(uname -m) in
    x86_64|amd64)
      echo "amd64"
      ;;
    aarch64|arm64)
      echo "arm64"
      ;;
    *)
      echo "❌ 不支持的架构: $(uname -m)" >&2
      exit 1
      ;;
  esac
}

# 从 GitHub API 获取下载链接
# 参数1: "stable" 或 "prerelease"
get_release_url() {
  local release_type=$1
  local API_URL

  if [[ "$release_type" == "stable" ]]; then
    API_URL="https://api.github.com/repos/$REPO/releases/latest"
    echo "🔍 正在查找最新的【稳定版】..."
  else
    # 获取所有 release 列表，最新的在最前面
    API_URL="https://api.github.com/repos/$REPO/releases"
    echo "🔍 正在查找最新的【构建版本】(包括预发布版)..."
  fi

  # 自动检测系统和架构
  OS=$(uname -s | tr '[:upper:]' '[:lower:]')
  ARCH=$(get_arch)
  
  # 根据平台构造期望的资源文件名
  ASSET_NAME="gost-${OS}-${ARCH}"
  
  echo "💻 当前系统: ${OS}-${ARCH}，需要文件: ${ASSET_NAME}"

  # 优先使用 jq，如果不存在则回退到 grep/cut
  if command -v jq &> /dev/null; then
    if [[ "$release_type" == "stable" ]]; then
      DOWNLOAD_URL=$(curl -s "$API_URL" | jq -r ".assets[] | select(.name == \"$ASSET_NAME\") | .browser_download_url")
    else
      # 从 release 列表中取第一个
      DOWNLOAD_URL=$(curl -s "$API_URL" | jq -r ".[0].assets[] | select(.name == \"$ASSET_NAME\") | .browser_download_url")
    fi
  else
    echo "⚠️ 警告: 未安装 jq，解析可能不稳定。建议安装 (e.g., sudo apt install jq)"
    if [[ "$release_type" == "stable" ]]; then
      DOWNLOAD_URL=$(curl -s "$API_URL" | grep "browser_download_url" | grep "$ASSET_NAME" | cut -d '"' -f 4 | head -n 1)
    else
      DOWNLOAD_URL=$(curl -s "$API_URL" | grep "browser_download_url" | grep "$ASSET_NAME" | cut -d '"' -f 4 | head -n 1)
    fi
  fi

  if [[ -z "$DOWNLOAD_URL" ]]; then
    echo "❌ 错误：在目标 Release 中未找到所需的文件 (${ASSET_NAME})。"
    echo "   请检查 GitHub Release 页面是否已上传该平台的文件。"
    exit 1
  fi
  
  echo "✅ 成功获取下载链接"
}

# 检查并安装 tcpkill
check_and_install_tcpkill() {
  # 检查 tcpkill 是否已安装
  if command -v tcpkill &> /dev/null; then
    return 0
  fi
  
  # 检测操作系统类型
  OS_TYPE=$(uname -s)
  
  # 检查是否需要 sudo
  if [[ $EUID -ne 0 ]]; then
    SUDO_CMD="sudo"
  else
    SUDO_CMD=""
  fi
  
  if [[ "$OS_TYPE" == "Darwin" ]]; then
    if command -v brew &> /dev/null; then
      brew install dsniff &> /dev/null
    fi
    return 0
  fi
  
  # 检测 Linux 发行版并安装对应的包
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO=$ID
  elif [ -f /etc/redhat-release ]; then
    DISTRO="rhel"
  elif [ -f /etc/debian_version ]; then
    DISTRO="debian"
  else
    return 0
  fi
  
  case $DISTRO in
    ubuntu|debian)
      $SUDO_CMD apt update &> /dev/null
      $SUDO_CMD apt install -y dsniff &> /dev/null
      ;;
    centos|rhel|fedora)
      if command -v dnf &> /dev/null; then
        $SUDO_CMD dnf install -y dsniff &> /dev/null
      elif command -v yum &> /dev/null; then
        $SUDO_CMD yum install -y dsniff &> /dev/null
      fi
      ;;
    alpine)
      $SUDO_CMD apk add --no-cache dsniff &> /dev/null
      ;;
    arch|manjaro)
      $SUDO_CMD pacman -S --noconfirm dsniff &> /dev/null
      ;;
    opensuse*|sles)
      $SUDO_CMD zypper install -y dsniff &> /dev/null
      ;;
    gentoo)
      $SUDO_CMD emerge --ask=n net-analyzer/dsniff &> /dev/null
      ;;
    void)
      $SUDO_CMD xbps-install -Sy dsniff &> /dev/null
      ;;
  esac
  
  return 0
}

# 获取用户输入的配置参数
get_config_params() {
  if [[ -z "$SERVER_ADDR" || -z "$SECRET" ]]; then
    echo "请输入配置参数："
    
    if [[ -z "$SERVER_ADDR" ]]; then
      read -p "服务器地址: " SERVER_ADDR
    fi
    
    if [[ -z "$SECRET" ]]; then
      read -p "密钥: " SECRET
    fi
    
    if [[ -z "$SERVER_ADDR" || -z "$SECRET" ]]; then
      echo "❌ 参数不完整，操作取消。"
      exit 1
    fi
  fi
}

# 解析命令行参数
while getopts "a:s:" opt; do
  case $opt in
    a) SERVER_ADDR="$OPTARG" ;;
    s) SECRET="$OPTARG" ;;
    *) echo "❌ 无效参数"; exit 1 ;;
  esac
done

# 安装或更新功能
# 参数1: "stable" 或 "prerelease"
install_or_update_gost() {
  local release_type=$1

  if [[ -d "$INSTALL_DIR" ]]; then
    echo "🔄 检测到 GOST 已安装，将执行更新操作..."
  else
    echo "🚀 开始全新安装 GOST..."
    get_config_params
  fi
  
  get_release_url "$release_type"
  
  echo ""
  echo "📥 检测到的下载地址为："
  echo "$DOWNLOAD_URL"
  read -p "是否有自己的加速下载地址？(留空则使用上述地址): " custom_url
  if [[ -n "$custom_url" ]]; then
    DOWNLOAD_URL="$custom_url"
    echo "✅ 使用自定义下载地址: $DOWNLOAD_URL"
  fi
  
  check_and_install_tcpkill
  mkdir -p "$INSTALL_DIR"

  if systemctl is-active --quiet gost; then
    echo "🛑 停止当前正在运行的 gost 服务..."
    systemctl stop gost
  fi

  echo "⬇️ 正在下载 gost..."
  if ! curl -L "$DOWNLOAD_URL" -o "$INSTALL_DIR/gost.new"; then
    echo "❌ 下载失败，请检查网络或下载链接。"
    exit 1
  fi
  
  if [[ ! -s "$INSTALL_DIR/gost.new" ]]; then
      echo "❌ 下载的文件为空，请检查下载链接。"
      rm -f "$INSTALL_DIR/gost.new"
      exit 1
  fi

  # 统一重命名为 gost
  echo "🔧 正在重命名文件为 'gost' 以确保兼容性..."
  mv "$INSTALL_DIR/gost.new" "$INSTALL_DIR/gost"
  chmod +x "$INSTALL_DIR/gost"
  echo "✅ 下载并准备完成"

  echo "🔎 当前 gost 版本：$($INSTALL_DIR/gost -V)"

  # 检查并创建 config.json
  if [[ ! -f "$INSTALL_DIR/config.json" ]]; then
    echo "📄 正在创建配置文件: config.json"
    cat > "$INSTALL_DIR/config.json" <<EOF
{
  "addr": "$SERVER_ADDR",
  "secret": "$SECRET"
}
EOF
  fi

  # 检查并创建 gost.json
  if [[ ! -f "$INSTALL_DIR/gost.json" ]]; then
    echo "📄 正在创建配置文件: gost.json"
    cat > "$INSTALL_DIR/gost.json" <<EOF
{}
EOF
  fi
  
  # 确保配置文件权限安全
  chmod 600 "$INSTALL_DIR"/*.json

  # 检查并创建 systemd 服务文件
  if [[ ! -f "/etc/systemd/system/gost.service" ]]; then
    echo "⚙️ 正在创建 systemd 服务..."
    # 使用 sudo 来确保有权限写入 /etc/systemd/system 目录
    if [[ $EUID -ne 0 ]]; then
      SUDO_CMD="sudo"
    else
      SUDO_CMD=""
    fi
    
    $SUDO_CMD tee "/etc/systemd/system/gost.service" > /dev/null <<EOF
[Unit]
Description=Gost Proxy Service
After=network.target

[Service]
WorkingDirectory=$INSTALL_DIR
ExecStart=$INSTALL_DIR/gost
Restart=on-failure
User=root
Group=root

[Install]
WantedBy=multi-user.target
EOF
    # 创建完服务文件后，需要重载 systemd 配置并启用服务
    $SUDO_CMD systemctl daemon-reload
    $SUDO_CMD systemctl enable gost
  fi

  echo "🚀 启动 gost 服务..."
  systemctl start gost

  echo "🔄 检查服务状态..."
  sleep 2
  if systemctl is-active --quiet gost; then
    echo "✅ 操作完成，gost 服务已成功启动！"
    echo "📁 配置目录: $INSTALL_DIR"
    echo "🔧 服务状态: $(systemctl is-active gost)"
  else
    echo "❌ gost 服务启动失败，请执行以下命令查看日志："
    echo "journalctl -u gost -f"
  fi
}

# 卸载功能
uninstall_gost() {
  echo "🗑️ 开始卸载 GOST..."
  
  read -p "确认卸载 GOST 吗？此操作将删除所有相关文件 (Y/N): " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "❌ 取消卸载"
    return 0
  fi

  # 停止并禁用服务
  if systemctl list-units --full -all | grep -Fq "gost.service"; then
    echo "🛑 停止并禁用服务..."
    systemctl stop gost 2>/dev/null
    systemctl disable gost 2>/dev/null
  fi

  # 删除服务文件
  if [[ -f "/etc/systemd/system/gost.service" ]]; then
    rm -f "/etc/systemd/system/gost.service"
    echo "🧹 删除服务文件"
  fi

  # 删除安装目录
  if [[ -d "$INSTALL_DIR" ]]; then
    rm -rf "$INSTALL_DIR"
    echo "🧹 删除安装目录: $INSTALL_DIR"
  fi

  # 重载 systemd
  systemctl daemon-reload

  echo "✅ 卸载完成"
}

# 主逻辑
main() {
  # 如果提供了命令行参数，直接执行安装
  if [[ -n "$SERVER_ADDR" && -n "$SECRET" ]]; then
    # 默认通过命令行安装时使用稳定版
    install_or_update_gost "stable"
    exit 0
  fi

  # 显示交互式菜单
  while true; do
    show_menu
    read -p "请输入选项 (1-4): " choice
    
    case $choice in
      1)
        install_or_update_gost "stable"
        break
        ;;
      2)
        install_or_update_gost "prerelease"
        break
        ;;
      3)
        uninstall_gost
        break
        ;;
      4)
        echo "👋 退出脚本"
        exit 0
        ;;
      *)
        echo "❌ 无效选项，请输入 1-4"
        echo ""
        ;;
    esac
  done
}

# 执行主函数
main