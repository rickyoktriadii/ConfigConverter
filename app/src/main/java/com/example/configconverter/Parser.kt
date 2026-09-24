package com.example.configconverter

import java.util.Base64
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.serialization.json.*

sealed class ParseResult {
    data class Ok(val config: ConnectionConfig): ParseResult()
    data class Error(val message: String): ParseResult()
}

object ConfigParser {
    fun parse(raw: String): ParseResult {
        val s = raw.trim().lines().firstOrNull { it.trim().isNotEmpty() }?.trim() ?: return ParseResult.Error("Empty configuration")
        return when {
            s.startsWith("vmess://", true) -> parseVmess(s)
            s.startsWith("vless://", true) -> parseVless(s)
            s.startsWith("trojan://", true) -> parseTrojan(s)
            else -> ParseResult.Error("Unknown / unsupported configuration")
        }
    }

    private fun decodeB64(s: String): String? {
        return try {
            val normalized = s.trim().replace("\n", "").replace("\r", "")
            val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
            String(Base64.decode(padded, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (_: Exception) { null }
    }

    private fun parseVmess(s: String): ParseResult {
        val decoded = decodeB64(s.removePrefix("vmess://")) ?: return ParseResult.Error("Invalid VMess Base64")
        val obj = try { Json.parseToJsonElement(decoded).jsonObject } catch (_: Exception) {
            return ParseResult.Error("Invalid JSON payload.")
        }
        fun str(k: String) = obj[k]?.jsonPrimitive?.contentOrNull ?: ""
        val server = str("add")
        val port = str("port").toIntOrNull()
        val uuid = str("id")
        if (server.isBlank()) return ParseResult.Error("Server address is missing.")
        if (port == null || port !in 1..65535) return ParseResult.Error("Port must be between 1 and 65535.")
        if (runCatching { UUID.fromString(uuid) }.isFailure) return ParseResult.Error("Missing or invalid UUID")
        val tlsValue = str("tls").lowercase()
        return ParseResult.Ok(ConnectionConfig(
            protocol="vmess", name=str("ps"), server=server, port=port, uuid=uuid,
            alterId=str("aid").toIntOrNull() ?: 0, cipher=str("scy").ifBlank { "auto" },
            network=str("net").ifBlank { "tcp" }, tls=tlsValue.isNotBlank() && tlsValue != "none",
            sni=str("sni").ifBlank { null }, host=str("host").ifBlank { null },
            path=str("path").ifBlank { null }, alpn=str("alpn").takeIf { it.isNotBlank() }?.split(",") ?: emptyList(),
            allowInsecure=str("allowInsecure").equals("1") || str("allowInsecure").equals("true", true),
            type=str("type").ifBlank { null }, security=tlsValue.ifBlank { null }
        ))
    }

    private fun parseUri(s: String, expected: String): ParseResult {
        val uri = try { URI(s) } catch (_: Exception) { return ParseResult.Error("Invalid $expected URI") }
        val userInfo = uri.rawUserInfo ?: return ParseResult.Error("Missing credentials")
        val parts = userInfo.split(":", limit = 2)
        val credential = try { URLDecoder.decode(parts[0], "UTF-8") } catch (_: Exception) { parts[0] }
        val host = uri.host ?: return ParseResult.Error("Server address is missing.")
        val port = if (uri.port == -1) 443 else uri.port
        if (port !in 1..65535) return ParseResult.Error("Port must be between 1 and 65535.")
        val query = mutableMapOf<String, String>()
        uri.rawQuery?.split("&")?.filter { it.isNotBlank() }?.forEach {
            val p = it.split("=", limit = 2)
            query[decode(p[0])] = if (p.size > 1) decode(p[1]) else ""
        }
        val name = uri.rawFragment?.let { decode(it) } ?: ""
        return ParseResult.Ok(
            if (expected == "VLESS") vlessConfig(credential, host, port, query, name)
            else trojanConfig(credential, host, port, query, name)
        )
    }

    private fun decode(v: String) = runCatching { URLDecoder.decode(v, "UTF-8") }.getOrElse { v }

    private fun vlessConfig(uuid: String, host: String, port: Int, q: Map<String,String>, name: String) =
        ConnectionConfig(
            protocol="vless", name=name, server=host, port=port, uuid=uuid,
            network=q["type"] ?: "tcp", security=q["security"], tls=q["security"].equals("tls", true) || q["security"].equals("reality", true),
            sni=q["sni"], host=q["host"], path=q["path"], serviceName=q["serviceName"] ?: q["serviceName"],
            flow=q["flow"], fingerprint=q["fp"] ?: q["fingerprint"], alpn=q["alpn"]?.split(",") ?: emptyList(),
            allowInsecure=q["allowInsecure"].equals("1") || q["allowInsecure"].equals("true", true),
            type=q["type"], cipher=q["encryption"] ?: "none"
        )

    private fun trojanConfig(password: String, host: String, port: Int, q: Map<String,String>, name: String) =
        ConnectionConfig(
            protocol="trojan", name=name, server=host, port=port, password=password,
            network=q["type"] ?: "tcp", security=q["security"] ?: "tls", tls=true,
            sni=q["sni"] ?: host, host=q["host"], path=q["path"], serviceName=q["serviceName"],
            fingerprint=q["fp"] ?: q["fingerprint"], alpn=q["alpn"]?.split(",") ?: emptyList(),
            allowInsecure=q["allowInsecure"].equals("1") || q["allowInsecure"].equals("true", true),
            type=q["type"]
        )

    private fun parseVless(s: String) = parseUri(s, "VLESS")
    private fun parseTrojan(s: String) = parseUri(s, "Trojan")
}
