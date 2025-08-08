package com.admin.common.utils;

import com.admin.common.dto.GostDto;
import com.admin.entity.Tunnel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class GostUtil {

    public static GostDto AddLimiters(Long node_id, Long name, String speed) {
        JSONObject data = createLimiterData(name, speed);
        return WebSocketServer.send_msg(node_id, data, "AddLimiters");
    }

    public static GostDto UpdateLimiters(Long node_id, Long name, String speed) {
        JSONObject data = createLimiterData(name, speed);
        JSONObject req = new JSONObject();
        req.put("limiter", String.valueOf(name));
        req.put("data", data);
        return WebSocketServer.send_msg(node_id, req, "UpdateLimiters");
    }

    public static GostDto DeleteLimiters(Long node_id, Long name) {
        JSONObject req = new JSONObject();
        req.put("limiter", String.valueOf(name));
        return WebSocketServer.send_msg(node_id, req, "DeleteLimiters");
    }

    public static GostDto AddService(Long node_id, String name, Integer in_port, Integer limiter, String remoteAddr, Integer fow_type, Tunnel tunnel, String strategy, String interfaceName) {
        JSONArray services = new JSONArray();
        String[] protocols = {"tcp", "udp"};
        for (String protocol : protocols) {
            JSONObject service = createServiceConfig(name, in_port, limiter, remoteAddr, protocol, fow_type, tunnel, strategy, interfaceName);
            services.add(service);
        }
        return WebSocketServer.send_msg(node_id, services, "AddService");
    }

    public static GostDto UpdateService(Long node_id, String name, Integer in_port, Integer limiter, String remoteAddr, Integer fow_type, Tunnel tunnel, String strategy, String interfaceName) {
        JSONArray services = new JSONArray();
        String[] protocols = {"tcp", "udp"};
        for (String protocol : protocols) {
            JSONObject service = createServiceConfig(name, in_port, limiter, remoteAddr, protocol, fow_type, tunnel, strategy, interfaceName);
            services.add(service);
        }
        return WebSocketServer.send_msg(node_id, services, "UpdateService");
    }

    public static GostDto DeleteService(Long node_id, String name) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        return WebSocketServer.send_msg(node_id, data, "DeleteService");
    }

    public static GostDto AddRemoteService(Long node_id, String name, Integer out_port, String remoteAddr, String protocol, String strategy, String interfaceName) {
        JSONObject data = new JSONObject();
        data.put("name", name + "_tls");
        data.put("addr", ":" + out_port);

        if (StringUtils.isNotBlank(interfaceName)) {
            JSONObject metadata = new JSONObject();
            metadata.put("interface", interfaceName);
            data.put("metadata", metadata);
        }

        JSONObject handler = new JSONObject();
        handler.put("type", "relay");
        data.put("handler", handler);

        JSONObject listener = new JSONObject();
        listener.put("type", protocol);
        data.put("listener", listener);

        JSONObject forwarder = createForwarder(remoteAddr, strategy);
        data.put("forwarder", forwarder);

        JSONArray services = new JSONArray();
        services.add(data);
        return WebSocketServer.send_msg(node_id, services, "AddService");
    }

    public static GostDto UpdateRemoteService(Long node_id, String name, Integer out_port, String remoteAddr, String protocol, String strategy, String interfaceName) {
        JSONObject data = new JSONObject();
        data.put("name", name + "_tls");
        data.put("addr", ":" + out_port);

        if (StringUtils.isNotBlank(interfaceName)) {
            JSONObject metadata = new JSONObject();
            metadata.put("interface", interfaceName);
            data.put("metadata", metadata);
        }

        JSONObject handler = new JSONObject();
        handler.put("type", "relay");
        data.put("handler", handler);

        JSONObject listener = new JSONObject();
        listener.put("type", protocol);
        data.put("listener", listener);

        JSONObject forwarder = createForwarder(remoteAddr, strategy);
        data.put("forwarder", forwarder);

        JSONArray services = new JSONArray();
        services.add(data);
        return WebSocketServer.send_msg(node_id, services, "UpdateService");
    }

    public static GostDto DeleteRemoteService(Long node_id, String name) {
        JSONArray data = new JSONArray();
        data.add(name + "_tls");
        JSONObject req = new JSONObject();
        req.put("services", data);
        return WebSocketServer.send_msg(node_id, req, "DeleteService");
    }

    public static GostDto PauseService(Long node_id, String name) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        return WebSocketServer.send_msg(node_id, data, "PauseService");
    }

    public static GostDto ResumeService(Long node_id, String name) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tcp");
        services.add(name + "_udp");
        data.put("services", services);
        return WebSocketServer.send_msg(node_id, data, "ResumeService");
    }

    public static GostDto PauseRemoteService(Long node_id, String name) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tls");
        data.put("services", services);
        return WebSocketServer.send_msg(node_id, data, "PauseService");
    }

    public static GostDto ResumeRemoteService(Long node_id, String name) {
        JSONObject data = new JSONObject();
        JSONArray services = new JSONArray();
        services.add(name + "_tls");
        data.put("services", services);
        return WebSocketServer.send_msg(node_id, data, "ResumeService");
    }

    // 链构建（兼容旧参数，不带 relayChain）
    public static GostDto AddChains(Long node_id, String name, String remoteAddr, String protocol, String interfaceName) {
        return AddChains(node_id, name, remoteAddr, protocol, interfaceName, null);
    }

    public static GostDto AddChains(Long node_id, String name, String remoteAddr, String protocol, String interfaceName, String relayChain) {
        JSONArray hops = buildHops(name, protocol, interfaceName, parseRelayAddresses(relayChain, remoteAddr));
        JSONObject data = new JSONObject();
        data.put("name", name + "_chains");
        data.put("hops", hops);
        return WebSocketServer.send_msg(node_id, data, "AddChains");
    }

    public static GostDto UpdateChains(Long node_id, String name, String remoteAddr, String protocol, String interfaceName) {
        return UpdateChains(node_id, name, remoteAddr, protocol, interfaceName, null);
    }

    public static GostDto UpdateChains(Long node_id, String name, String remoteAddr, String protocol, String interfaceName, String relayChain) {
        JSONArray hops = buildHops(name, protocol, interfaceName, parseRelayAddresses(relayChain, remoteAddr));
        JSONObject data = new JSONObject();
        data.put("name", name + "_chains");
        data.put("hops", hops);
        JSONObject req = new JSONObject();
        req.put("chain", name + "_chains");
        req.put("data", data);
        return WebSocketServer.send_msg(node_id, req, "UpdateChains");
    }

    public static GostDto DeleteChains(Long node_id, String name) {
        JSONObject data = new JSONObject();
        data.put("chain", name + "_chains");
        return WebSocketServer.send_msg(node_id, data, "DeleteChains");
    }

    private static JSONObject createLimiterData(Long name, String speed) {
        JSONObject data = new JSONObject();
        data.put("name", name.toString());
        JSONArray limits = new JSONArray();
        limits.add("$ " + speed + "MB " + speed + "MB");
        data.put("limits", limits);
        return data;
    }

    private static JSONObject createServiceConfig(String name, Integer in_port, Integer limiter, String remoteAddr, String protocol, Integer fow_type, Tunnel tunnel, String strategy, String interfaceName) {
        JSONObject service = new JSONObject();
        service.put("name", name + "_" + protocol);
        if (Objects.equals(protocol, "tcp")) {
            service.put("addr", tunnel.getTcpListenAddr() + ":" + in_port);
        } else {
            service.put("addr", tunnel.getUdpListenAddr() + ":" + in_port);
        }

        if (StringUtils.isNotBlank(interfaceName)) {
            JSONObject metadata = new JSONObject();
            metadata.put("interface", interfaceName);
            service.put("metadata", metadata);
        }


        // 添加限流器配置
        if (limiter != null) {
            service.put("limiter", limiter.toString());
        }

        // 配置处理器
        JSONObject handler = createHandler(protocol, name, fow_type);
        service.put("handler", handler);

        // 配置监听器
        JSONObject listener = createListener(protocol);
        service.put("listener", listener);

        // 端口转发需要配置转发器
        if (isPortForwarding(fow_type)) {
            JSONObject forwarder = createForwarder(remoteAddr, strategy);
            service.put("forwarder", forwarder);
        }
        return service;
    }

    private static JSONObject createHandler(String protocol, String name, Integer fow_type) {
        JSONObject handler = new JSONObject();
        handler.put("type", protocol);

        // 隧道转发需要添加链配置
        if (isTunnelForwarding(fow_type)) {
            handler.put("chain", name + "_chains");
        }

        return handler;
    }

    private static JSONObject createListener(String protocol) {
        JSONObject listener = new JSONObject();
        listener.put("type", protocol);
        if (Objects.equals(protocol, "udp")){
        if (Objects.equals(protocol, "udp")) {
            JSONObject metadata = new JSONObject();
            metadata.put("keepAlive", true);
            listener.put("metadata", metadata);
        }
        return listener;
    }

    private static JSONObject createForwarder(String remoteAddr, String strategy) {
        JSONObject forwarder = new JSONObject();
        JSONArray nodes = new JSONArray();

        String[] split = remoteAddr.split(",");
        int num = 1;
        for (String addr : split) {
            JSONObject node = new JSONObject();
            node.put("name", "node_" + num);
            node.put("addr", addr);
            nodes.add(node);
            num++;
        }
        if (StringUtils.isBlank(strategy)) {
            strategy = "fifo";
        }

        forwarder.put("nodes", nodes);

        JSONObject selector = new JSONObject();
        selector.put("strategy", strategy);
        selector.put("maxFails", 1);
        selector.put("failTimeout", "600s");
        forwarder.put("selector", selector);
        return forwarder;
    }

    private static boolean isPortForwarding(Integer fow_type) {
        return fow_type != null && fow_type == 1;
    }

    private static boolean isTunnelForwarding(Integer fow_type) {
        return fow_type != null && fow_type != 1;
    }

    // ===== Helpers for multi-hop chains =====
    private static JSONArray buildHops(String name, String protocol, String interfaceName, java.util.List<String> addrs) {
        JSONArray hops = new JSONArray();
        int hopIdx = 1;
        for (String addr : addrs) {
            if (StringUtils.isBlank(addr)) continue;

            JSONObject connector = new JSONObject();
            connector.put("type", "relay");

            JSONObject dialer = new JSONObject();
            dialer.put("type", protocol);
            if (Objects.equals(protocol, "quic")) {
                JSONObject md = new JSONObject();
                md.put("keepAlive", true);
                md.put("ttl", "10s");
                dialer.put("metadata", md);
            }

            JSONObject node = new JSONObject();
            node.put("name", "node-" + name + "-" + hopIdx);
            node.put("addr", addr.trim());
            node.put("connector", connector);
            node.put("dialer", dialer);
            if (StringUtils.isNotBlank(interfaceName)) {
                node.put("interface", interfaceName);
            }

            JSONArray nodes = new JSONArray();
            nodes.add(node);

            JSONObject hop = new JSONObject();
            hop.put("name", "hop-" + name + "-" + hopIdx);
            hop.put("nodes", nodes);
            hops.add(hop);
            hopIdx++;
        }
        return hops;
    }

    private static java.util.List<String> parseRelayAddresses(String relayChain, String finalRemoteAddr) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (StringUtils.isNotBlank(relayChain)) {
            String[] lines = relayChain.split("\r?\n");
            for (String line : lines) {
                String addr = StringUtils.trim(line);
                if (StringUtils.isNotBlank(addr)) {
                    result.add(normalizeHostPort(addr));
                }
            }
        }
        if (StringUtils.isNotBlank(finalRemoteAddr)) {
            result.add(normalizeHostPort(finalRemoteAddr.trim()));
        }
        return result;
    }

    /**
     * 规范化 host:port 表达式：
     * - 若为未加[]的IPv6地址，自动加[]（通过统计冒号数判断）
     * - 已加[]或域名/IPv4不处理
     */
    private static String normalizeHostPort(String addr) {
        if (StringUtils.isBlank(addr)) return addr;
        String s = addr.trim();
        if (s.startsWith("[")) return s; // 已经加了[]

        int lastColon = s.lastIndexOf(':');
        if (lastColon < 0) return s; // 没有端口分隔符，原样返回

        String host = s.substring(0, lastColon);
        String port = s.substring(lastColon + 1);

        // 仅在host中包含多于1个冒号时视为未加[]的IPv6
        int colonCount = 0;
        for (int i = 0; i < host.length(); i++) {
            if (host.charAt(i) == ':') colonCount++;
        }
        if (colonCount > 1) {
            return "[" + host + "]:" + port;
        }
        return s;
    }
}
