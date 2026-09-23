package com.example.configconverter

import kotlinx.serialization.json.*

object V2RayGenerator {
    fun generate(c: ConnectionConfig): String {
        val outbound = buildJsonObject {
            put("protocol", c.protocol)
            putJsonObject("settings") {
                when (c.protocol) {
                    "vmess" -> putJsonArray("vnext") {
                        addJsonObject {
                            put("address", c.server); put("port", c.port)
                            putJsonArray("users") {
                                addJsonObject {
                                    put("id", c.uuid ?: "")
                                    put("alterId", c.alterId)
                                    put("security", c.cipher)
                                }
                            }
                        }
                    }
                    "vless" -> putJsonArray("vnext") {
                        addJsonObject {
                            put("address", c.server); put("port", c.port)
                            putJsonArray("users") {
                                addJsonObject {
                                    put("id", c.uuid ?: "")
                                    put("encryption", c.cipher)
                                    c.flow?.let { put("flow", it) }
                                }
                            }
                        }
                    }
                    "trojan" -> putJsonArray("servers") {
                        addJsonObject { put("address", c.server); put("port", c.port); put("password", c.password ?: "") }
                    }
                }
            }
            putJsonObject("streamSettings") {
                put("network", c.network)
                if (c.tls || c.security.equals("tls", true) || c.security.equals("reality", true)) {
                    put("security", c.security?.ifBlank { "tls" } ?: "tls")
                    putJsonObject("tlsSettings") {
                        c.sni?.let { put("serverName", it) }
                        if (c.alpn.isNotEmpty()) putJsonArray("alpn") { c.alpn.forEach(::add) }
                        put("allowInsecure", c.allowInsecure)
                    }
                }
                when (c.network.lowercase()) {
                    "ws" -> putJsonObject("wsSettings") {
                        put("path", c.path ?: "/")
                        if (!c.host.isNullOrBlank()) putJsonObject("headers") { put("Host", c.host!!) }
                    }
                    "grpc" -> putJsonObject("grpcSettings") { put("serviceName", c.serviceName ?: "") }
                }
            }
        }
        return Json { prettyPrint = true; prettyPrintIndent = "  " }.encodeToString(JsonObject.serializer(), buildJsonObject {
            putJsonArray("outbounds") { add(outbound) }
        })
    }
}

object OpenClashGenerator {
    fun generate(c: ConnectionConfig): String {
        val sb = StringBuilder("proxies:\n")
        sb.append("  - name: ").append(yaml(c.name.ifBlank { c.protocol.uppercase() })).append('\n')
        sb.append("    type: ").append(c.protocol).append('\n')
        sb.append("    server: ").append(yaml(c.server)).append('\n')
        sb.append("    port: ").append(c.port).append('\n')
        when (c.protocol) {
            "vmess", "vless" -> {
                sb.append("    uuid: ").append(yaml(c.uuid ?: "")).append('\n')
                if (c.protocol == "vmess") sb.append("    alterId: ").append(c.alterId).append('\n')
                sb.append("    cipher: ").append(yaml(if (c.protocol == "vless") "none" else c.cipher.ifBlank { "auto" })).append('\n')
            }
            "trojan" -> sb.append("    password: ").append(yaml(c.password ?: "")).append('\n')
        }
        if (c.tls) sb.append("    tls: true\n")
        c.sni?.takeIf { it.isNotBlank() }?.let { sb.append("    servername: ").append(yaml(it)).append('\n') }
        sb.append("    network: ").append(c.network).append('\n')
        when (c.network.lowercase()) {
            "ws" -> {
                sb.append("    ws-opts:\n")
                sb.append("      path: ").append(yaml(c.path ?: "/")).append('\n')
                c.host?.takeIf { it.isNotBlank() }?.let {
                    sb.append("      headers:\n        Host: ").append(yaml(it)).append('\n')
                }
            }
            "grpc" -> sb.append("    grpc-opts:\n      grpc-service-name: ").append(yaml(c.serviceName ?: "")).append('\n')
        }
        if (c.allowInsecure) sb.append("    skip-cert-verify: true\n")
        if (c.udp) sb.append("    udp: true\n")
        return sb.toString()
    }

    private fun yaml(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
