package com.example.configconverter

import java.util.UUID

object ConfigValidator {
    fun validate(c: ConnectionConfig): String? {
        if (c.server.isBlank()) return "Server address is missing."
        if (c.port !in 1..65535) return "Port must be between 1 and 65535."
        if (c.protocol == "vmess" || c.protocol == "vless") {
            val id = c.uuid ?: return "Missing UUID"
            if (runCatching { UUID.fromString(id) }.isFailure) return "Invalid UUID"
        }
        if (c.protocol == "trojan" && c.password.isNullOrEmpty()) return "Missing Trojan password"
        val supported = setOf(
    "tcp", "raw", "ws", "websocket",
    "grpc", "http", "h2", "httpupgrade",
    "splithttp", "xhttp", "hysteria"
)
if (
    c.protocol !in setOf(
        "shadowsocks",
        "socks",
        "http",
        "wireguard"
    ) &&
    c.network.lowercase() !in supported
) {
    return "Unsupported transport type: ${c.network}"
}
        return null
    }
}
