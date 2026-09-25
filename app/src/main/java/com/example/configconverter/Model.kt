package com.example.configconverter

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    val protocol: String,
    val name: String = "",
    val server: String,
    val port: Int,

    val uuid: String? = null,
    val password: String? = null,

    val alterId: Int = 0,
    val cipher: String = "auto",

    val network: String = "tcp",
    val security: String? = null,

    val tls: Boolean = false,
    val sni: String? = null,

    val host: String? = null,
    val path: String? = null,
    val serviceName: String? = null,

    val flow: String? = null,
    val fingerprint: String? = null,

    val alpn: List<String> = emptyList(),

    val allowInsecure: Boolean = false,
    val udp: Boolean = true,

    val type: String? = null,

    // REALITY
    val realityPublicKey: String? = null,
    val realityShortId: String? = null,
    val realitySpiderX: String? = null
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
