package com.example.configconverter

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    val protocol: String,

    val name: String = "",

    val server: String,
    val port: Int,

    // Common authentication
    val uuid: String? = null,
    val password: String? = null,
    val username: String? = null,

    // VMess
    val alterId: Int = 0,
    val cipher: String = "auto",

    // Transport
    val network: String = "tcp",
    val security: String? = null,

    val tls: Boolean = false,
    val sni: String? = null,

    // WebSocket / HTTP
    val host: String? = null,
    val path: String? = null,

    // gRPC
    val serviceName: String? = null,

    // VLESS
    val flow: String? = null,
    val encryption: String? = null,

    // TLS
    val fingerprint: String? = null,
    val alpn: List<String> = emptyList(),

    val allowInsecure: Boolean = false,
    val udp: Boolean = true,

    val type: String? = null,

    // REALITY
    val realityPublicKey: String? = null,
    val realityShortId: String? = null,
    val realitySpiderX: String? = null,

    // Shadowsocks
    val method: String? = null,

    // WireGuard
    val privateKey: String? = null,
    val publicKey: String? = null,
    val peerPublicKey: String? = null,
    val allowedIPs: List<String> = emptyList(),
    val localAddress: List<String> = emptyList(),
    val mtu: Int? = null,

    // Hysteria / Hysteria2
    val hysteriaVersion: Int = 2,
    val obfs: String? = null,
    val obfsPassword: String? = null,
    val upMbps: Int? = null,
    val downMbps: Int? = null,

    // Optional HTTP headers
    val headers: Map<String, String> = emptyMap()
)

data class ConversionResult(
    val config: ConnectionConfig,
    val v2rayJson: String,
    val openClashYaml: String
)

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: String,
    val server: String,
    val port: Int
)
