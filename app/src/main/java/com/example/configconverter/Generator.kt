package com.example.configconverter

import kotlinx.serialization.json.*

object V2RayGenerator {

    fun generate(c: ConnectionConfig): String {

        val streamSettings = buildJsonObject {

            val method = when (c.network.lowercase()) {
                "ws", "websocket" -> "websocket"
                "grpc" -> "grpc"
                "tcp", "raw" -> "raw"
                "http" -> "http"
                "h2" -> "http"
                else -> c.network
            }

            put("network", method)
            put("security", c.security ?: if (c.tls) "tls" else "none")

            when {
                c.security.equals("reality", true) -> {
                    putJsonObject("realitySettings") {

                        c.sni
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("serverName", it) }

                        c.fingerprint
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("fingerprint", it) }

                        c.realityPublicKey
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("password", it) }

                        c.realityShortId
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("shortId", it) }

                        c.realitySpiderX
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("spiderX", it) }
                    }
                }

                c.security.equals("tls", true) ||
                c.tls -> {
                    putJsonObject("tlsSettings") {

                        c.sni
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("serverName", it) }

                        if (c.alpn.isNotEmpty()) {
                            putJsonArray("alpn") {
                                c.alpn.forEach {
                                    add(it)
                                }
                            }
                        }

                        c.fingerprint
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("fingerprint", it) }

                        put(
                            "allowInsecure",
                            c.allowInsecure
                        )
                    }
                }
            }

            when (method) {

                "websocket" -> {
                    putJsonObject("wsSettings") {

                        put(
                            "path",
                            c.path ?: "/"
                        )

                        if (!c.host.isNullOrBlank()) {
                            putJsonObject("headers") {
                                put(
                                    "Host",
                                    c.host
                                )
                            }
                        }
                    }
                }

                "grpc" -> {
                    putJsonObject("grpcSettings") {

                        put(
                            "serviceName",
                            c.serviceName ?: ""
                        )
                    }
                }
            }
        }

        val outbound = buildJsonObject {

            put(
                "protocol",
                c.protocol
            )

            putJsonObject("settings") {

                when (c.protocol.lowercase()) {

                    "vmess" -> {
                        putJsonArray("vnext") {
                            addJsonObject {

                                put(
                                    "address",
                                    c.server
                                )

                                put(
                                    "port",
                                    c.port
                                )

                                putJsonArray("users") {
                                    addJsonObject {

                                        put(
                                            "id",
                                            c.uuid ?: ""
                                        )

                                        put(
                                            "alterId",
                                            c.alterId
                                        )

                                        put(
                                            "security",
                                            c.cipher.ifBlank {
                                                "auto"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "vless" -> {
                        putJsonArray("vnext") {
                            addJsonObject {

                                put(
                                    "address",
                                    c.server
                                )

                                put(
                                    "port",
                                    c.port
                                )

                                putJsonArray("users") {
                                    addJsonObject {

                                        put(
                                            "id",
                                            c.uuid ?: ""
                                        )

                                        put(
                                            "encryption",
                                            c.cipher.ifBlank {
                                                "none"
                                            }
                                        )

                                        c.flow
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?.let {
                                                put(
                                                    "flow",
                                                    it
                                                )
                                            }
                                    }
                                }
                            }
                        }
                    }

                    "trojan" -> {
                        putJsonArray("servers") {
                            addJsonObject {

                                put(
                                    "address",
                                    c.server
                                )

                                put(
                                    "port",
                                    c.port
                                )

                                put(
                                    "password",
                                    c.password ?: ""
                                )
                            }
                        }
                    }
                }
            }

            put(
                "streamSettings",
                streamSettings
            )

            if (c.udp) {
                putJsonObject("mux") {
                    put(
                        "enabled",
                        false
                    )
                }
            }
        }

        return Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                putJsonArray("outbounds") {
                    add(outbound)
                }
            }
        )
    }
}

object OpenClashGenerator {

    fun generate(c: ConnectionConfig): String {

        val sb = StringBuilder()

        sb.append("proxies:\n")

        sb.append("  - name: ")
            .append(
                yaml(
                    c.name.ifBlank {
                        c.protocol.uppercase()
                    }
                )
            )
            .append('\n')

        sb.append("    type: ")
            .append(c.protocol.lowercase())
            .append('\n')

        sb.append("    server: ")
            .append(yaml(c.server))
            .append('\n')

        sb.append("    port: ")
            .append(c.port)
            .append('\n')

        when (c.protocol.lowercase()) {

            "vmess",
            "vless" -> {

                sb.append("    uuid: ")
                    .append(yaml(c.uuid ?: ""))
                    .append('\n')

                if (c.protocol.equals("vmess", true)) {
                    sb.append("    alterId: ")
                        .append(c.alterId)
                        .append('\n')

                    sb.append("    cipher: ")
                        .append(
                            yaml(
                                c.cipher.ifBlank {
                                    "auto"
                                }
                            )
                        )
                        .append('\n')
                } else {
                    sb.append("    cipher: none\n")
                }

                c.flow
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        sb.append("    flow: ")
                            .append(yaml(it))
                            .append('\n')
                    }
            }

            "trojan" -> {
                sb.append("    password: ")
                    .append(yaml(c.password ?: ""))
                    .append('\n')
            }
        }

        if (
            c.tls ||
            c.security.equals("tls", true) ||
            c.security.equals("reality", true)
        ) {
            sb.append("    tls: true\n")
        }

        c.sni
            ?.takeIf { it.isNotBlank() }
            ?.let {
                sb.append("    servername: ")
                    .append(yaml(it))
                    .append('\n')
            }

        c.fingerprint
            ?.takeIf { it.isNotBlank() }
            ?.let {
                sb.append("    client-fingerprint: ")
                    .append(yaml(it))
                    .append('\n')
            }

        if (c.alpn.isNotEmpty()) {

            sb.append("    alpn:\n")

            c.alpn.forEach {
                sb.append("      - ")
                    .append(yaml(it))
                    .append('\n')
            }
        }

        sb.append("    network: ")
            .append(
                when (c.network.lowercase()) {
                    "websocket" -> "ws"
                    else -> c.network.lowercase()
                }
            )
            .append('\n')

        when (c.network.lowercase()) {

            "ws",
            "websocket" -> {

                sb.append("    ws-opts:\n")

                sb.append("      path: ")
                    .append(yaml(c.path ?: "/"))
                    .append('\n')

                c.host
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        sb.append(
                            "      headers:\n"
                        )

                        sb.append(
                            "        Host: "
                        )
                            .append(yaml(it))
                            .append('\n')
                    }
            }

            "grpc" -> {

                sb.append("    grpc-opts:\n")

                sb.append(
                    "      grpc-service-name: "
                )
                    .append(
                        yaml(
                            c.serviceName ?: ""
                        )
                    )
                    .append('\n')
            }
        }

        if (
            c.security.equals(
                "reality",
                true
            )
        ) {

            sb.append("    reality-opts:\n")

            c.realityPublicKey
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    sb.append(
                        "      public-key: "
                    )
                        .append(yaml(it))
                        .append('\n')
                }

            c.realityShortId
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    sb.append(
                        "      short-id: "
                    )
                        .append(yaml(it))
                        .append('\n')
                }
        }

        if (c.allowInsecure) {
            sb.append(
                "    skip-cert-verify: true\n"
            )
        }

        if (c.udp) {
            sb.append(
                "    udp: true\n"
            )
        }

        return sb.toString()
    }

    private fun yaml(s: String): String {
        return "\"" +
            s.replace(
                "\\",
                "\\\\"
            )
                .replace(
                    "\"",
                    "\\\""
                )
                .replace(
                    "\n",
                    "\\n"
                ) +
            "\""
    }
}
