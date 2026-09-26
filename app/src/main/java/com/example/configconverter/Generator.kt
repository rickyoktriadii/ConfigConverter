package com.example.configconverter

import kotlinx.serialization.json.*

object V2RayGenerator {

     fun generate(c: ConnectionConfig): String {
    return when (c.protocol.lowercase()) {
        "vmess" -> vmess(c)
        "vless" -> vless(c)
        "trojan" -> trojan(c)
        "shadowsocks" -> shadowsocks(c)
        "socks" -> socks(c)
        "http" -> http(c)
        "wireguard" -> wireguard(c)
        "hysteria" -> hysteria(c)
        else -> error("Unsupported protocol: ${c.protocol}")
    }.toString()
  }

    private fun base(
        protocol: String,
        settings: JsonObject,
        stream: JsonObject? = null
    ): JsonObject {
        return buildJsonObject {
            put("protocol", protocol)
            put("settings", settings)
            if (stream != null) {
                put("streamSettings", stream)
            }
        }
    }

    private fun stream(c: ConnectionConfig): JsonObject {
        return buildJsonObject {
            put("method", c.network.ifBlank { "raw" })

            if (c.tls || !c.security.isNullOrBlank()) {
                put(
                    "security",
                    c.security ?: "tls"
                )
            }

            if (!c.sni.isNullOrBlank()) {
                putJsonObject("tlsSettings") {
                    put("serverName", c.sni)
                    put(
                        "allowInsecure",
                        c.allowInsecure
                    )

                    if (c.fingerprint != null) {
                        put(
                            "fingerprint",
                            c.fingerprint
                        )
                    }

                    if (c.alpn.isNotEmpty()) {
                        putJsonArray("alpn") {
                            c.alpn.forEach {
                                add(it)
                            }
                        }
                    }
                }
            }

            if (
                c.network.equals("ws", true) ||
                c.network.equals("websocket", true)
            ) {
                putJsonObject("wsSettings") {
                    put(
                        "path",
                        c.path ?: "/"
                    )

                    if (!c.host.isNullOrBlank()) {
                        putJsonObject("headers") {
                            put("Host", c.host)
                        }
                    }
                }
            }

            if (c.network.equals("grpc", true)) {
                putJsonObject("grpcSettings") {
                    put(
                        "serviceName",
                        c.serviceName ?: ""
                    )
                }
            }

            if (
                c.security.equals(
                    "reality",
                    true
                )
            ) {
                putJsonObject("realitySettings") {
                    put(
                        "serverName",
                        c.sni ?: c.server
                    )

                    put(
                        "fingerprint",
                        c.fingerprint ?: "chrome"
                    )

                    put(
                        "publicKey",
                        c.realityPublicKey ?: ""
                    )

                    put(
                        "shortId",
                        c.realityShortId ?: ""
                    )

                    put(
                        "spiderX",
                        c.realitySpiderX ?: ""
                    )
                }
            }
        }
    }
    
    private fun vmess(c: ConnectionConfig): JsonObject {
        return base(
            "vmess",
            buildJsonObject {
                putJsonArray("vnext") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)

                        putJsonArray("users") {
                            addJsonObject {
                                put("id", c.uuid ?: "")
                                put("alterId", c.alterId)
                                put("security", c.cipher)
                            }
                        }
                    }
                }
            },
            stream(c)
        )
    }

    private fun vless(c: ConnectionConfig): JsonObject {
        return base(
            "vless",
            buildJsonObject {
                putJsonArray("vnext") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)

                        putJsonArray("users") {
                            addJsonObject {
                                put("id", c.uuid ?: "")
                                put("encryption", c.encryption ?: "none")

                                if (!c.flow.isNullOrBlank()) {
                                    put("flow", c.flow)
                                }
                            }
                        }
                    }
                }
            },
            stream(c)
        )
    }

    private fun trojan(c: ConnectionConfig): JsonObject {
        return base(
            "trojan",
            buildJsonObject {
                putJsonArray("servers") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)
                        put("password", c.password ?: "")

                        if (c.flow != null) {
                            put("flow", c.flow)
                        }
                    }
                }
            },
            stream(c)
        )
    }
    
        private fun shadowsocks(c: ConnectionConfig): JsonObject {
        return base(
            "shadowsocks",
            buildJsonObject {
                putJsonArray("servers") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)
                        put("method", c.method ?: c.cipher)
                        put("password", c.password ?: "")
                    }
                }
            }
        )
    }

    private fun socks(c: ConnectionConfig): JsonObject {
        return base(
            "socks",
            buildJsonObject {
                putJsonArray("servers") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)

                        if (
                            !c.username.isNullOrBlank() ||
                            !c.password.isNullOrBlank()
                        ) {
                            putJsonObject("users") {
                                put(
                                    "user",
                                    c.username ?: ""
                                )
                                put(
                                    "pass",
                                    c.password ?: ""
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    private fun http(c: ConnectionConfig): JsonObject {
        return base(
            "http",
            buildJsonObject {
                putJsonArray("servers") {
                    addJsonObject {
                        put("address", c.server)
                        put("port", c.port)

                        if (!c.username.isNullOrBlank()) {
                            putJsonArray("users") {
                                addJsonObject {
                                    put(
                                        "user",
                                        c.username
                                    )
                                    put(
                                        "pass",
                                        c.password ?: ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

        private fun wireguard(c: ConnectionConfig): JsonObject {
        return base(
            "wireguard",
            buildJsonObject {
                put("secretKey", c.privateKey ?: "")

                if (c.mtu != null) {
                    put("mtu", c.mtu)
                }

                putJsonArray("address") {
                    c.localAddress.forEach {
                        add(it)
                    }
                }

                putJsonArray("peers") {
                    addJsonObject {
                        put(
                            "publicKey",
                            c.peerPublicKey ?: c.publicKey ?: ""
                        )
                        put("endpoint", "${c.server}:${c.port}")

                        putJsonArray("allowedIPs") {
                            if (c.allowedIPs.isEmpty()) {
                                add("0.0.0.0/0")
                                add("::/0")
                            } else {
                                c.allowedIPs.forEach {
                                    add(it)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun hysteria(c: ConnectionConfig): JsonObject {
        return base(
            "hysteria",
            buildJsonObject {
                put("version", c.hysteriaVersion)
                put("server", "${c.server}:${c.port}")

                if (!c.password.isNullOrBlank()) {
                    put("auth", c.password)
                }

                if (!c.sni.isNullOrBlank()) {
                    put("serverName", c.sni)
                }

                put(
                    "insecure",
                    c.allowInsecure
                )

                if (c.upMbps != null) {
                    put("upMbps", c.upMbps)
                }

                if (c.downMbps != null) {
                    put("downMbps", c.downMbps)
                }

                if (!c.obfs.isNullOrBlank()) {
                    put("obfs", c.obfs)

                    if (!c.obfsPassword.isNullOrBlank()) {
                        put(
                            "obfsPassword",
                            c.obfsPassword
                        )
                    }
                }
            }
        )
    }
}

object OpenClashGenerator {

    fun generate(c: ConnectionConfig): String {
        val name = c.name.ifBlank {
            "${c.protocol}-${c.server}"
        }

        return buildString {
            appendLine("proxies:")
            appendLine("  - name: \"$name\"")
            appendLine("    type: ${c.protocol.lowercase()}")
            appendLine("    server: ${c.server}")
            appendLine("    port: ${c.port}")

            if (!c.uuid.isNullOrBlank()) {
                appendLine("    uuid: ${c.uuid}")
            }

            if (!c.password.isNullOrBlank()) {
                appendLine("    password: ${c.password}")
            }

            if (!c.username.isNullOrBlank()) {
                appendLine("    username: ${c.username}")
            }

            if (c.tls) {
                appendLine("    tls: true")
            }

            if (!c.sni.isNullOrBlank()) {
                appendLine("    servername: ${c.sni}")
            }

            if (!c.path.isNullOrBlank()) {
                appendLine("    ws-opts:")
                appendLine("      path: ${c.path}")
            }
        }
    }
}
