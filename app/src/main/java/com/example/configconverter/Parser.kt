package com.example.configconverter

import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.*

sealed class ParseResult {
    data class Ok(val config: ConnectionConfig): ParseResult()
    data class Error(val message: String): ParseResult()
}

object ConfigParser {

    fun parse(raw: String): ParseResult {
        val s = raw.trim().lines()
            .firstOrNull { it.isNotBlank() }
            ?.trim() ?: return ParseResult.Error("Empty configuration")

        return when {
            s.startsWith("{") && s.contains("\"outbounds\"") ->
                xray(s)
            s.startsWith("vmess://", true) ->
                vmess(s)
            s.startsWith("vless://", true) ->
                uri(s, "vless")
            s.startsWith("trojan://", true) ->
                uri(s, "trojan")
            s.startsWith("ss://", true) ->
                ss(s)
            s.startsWith("socks://", true) ||
            s.startsWith("socks5://", true) ->
                socks(s)
            s.startsWith("http://", true) ||
            s.startsWith("https://", true) ->
                http(s)
            s.startsWith("wireguard://", true) ->
                wireguard(s)
            s.startsWith("hysteria2://", true) ||
            s.startsWith("hy2://", true) ||
            s.startsWith("hysteria://", true) ->
                hysteria(s)
            else ->
                ParseResult.Error("Unknown / unsupported configuration")
        }
    }

    private fun b64(s: String): String? = try {
        val x = s.trim().replace("\n","").replace("\r","")
        String(
            Base64.getDecoder().decode(
                x + "=".repeat((4-x.length%4)%4)
            ),
            StandardCharsets.UTF_8
        )
    } catch (_: Exception) { null }

    private fun dec(s: String): String =
        runCatching {
            URLDecoder.decode(s, "UTF-8")
        }.getOrElse { s }

    private fun query(u: URI): Map<String,String> =
        u.rawQuery?.split("&")
            ?.filter { it.isNotBlank() }
            ?.associate {
                val p = it.split("=", limit=2)
                dec(p[0]) to if (p.size > 1) dec(p[1]) else ""
            } ?: emptyMap()

    private fun port(u: URI, d: Int = 443): Int =
        if (u.port == -1) d else u.port
        private fun vmess(s: String): ParseResult {
        val o = b64(s.substringAfter("://"))
            ?: return ParseResult.Error("Invalid VMess Base64")

        val j = runCatching {
            Json.parseToJsonElement(o).jsonObject
        }.getOrElse {
            return ParseResult.Error("Invalid VMess JSON")
        }

        fun v(k: String) =
            j[k]?.jsonPrimitive?.contentOrNull ?: ""

        val p = v("port").toIntOrNull()
            ?: return ParseResult.Error("Invalid VMess port")

        if (v("add").isBlank())
            return ParseResult.Error("VMess server missing")

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="vmess",
                name=v("ps"),
                server=v("add"),
                port=p,
                uuid=v("id"),
                alterId=v("aid").toIntOrNull() ?: 0,
                cipher=v("scy").ifBlank{"auto"},
                network=v("net").ifBlank{"tcp"},
                security=v("tls").ifBlank{null},
                tls=v("tls").isNotBlank() &&
                    !v("tls").equals("none",true),
                sni=v("sni").ifBlank{null},
                host=v("host").ifBlank{null},
                path=v("path").ifBlank{null},
                serviceName=v("serviceName").ifBlank{null},
                fingerprint=v("fp").ifBlank{null},
                alpn=v("alpn").split(",")
                    .map{it.trim()}
                    .filter{it.isNotBlank()}
            )
        )
    }

    private fun uri(
        s: String,
        protocol: String
    ): ParseResult {

        val u = runCatching { URI(s) }.getOrElse {
            return ParseResult.Error("Invalid $protocol URI")
        }

        val host = u.host
            ?: return ParseResult.Error("Server missing")

        val q = query(u)
        val user = u.rawUserInfo
            ?.substringBefore(":")
            ?.let(::dec)
            ?: return ParseResult.Error("Credentials missing")

        val sec = q["security"]?.lowercase()
        val net = q["type"]?.lowercase() ?: "tcp"

        return ParseResult.Ok(
            ConnectionConfig(
                protocol=protocol,
                name=u.rawFragment?.let(::dec) ?: "",
                server=host,
                port=port(u),
                uuid=if(protocol=="vless") user else null,
                password=if(protocol=="trojan") user else null,
                network=net,
                security=sec ?: if(protocol=="trojan") "tls" else null,
                tls=protocol=="trojan" ||
                    sec.equals("tls",true) ||
                    sec.equals("reality",true),
                sni=q["sni"],
                host=q["host"],
                path=q["path"],
                serviceName=q["serviceName"],
                flow=q["flow"],
                encryption=q["encryption"] ?: "none",
                cipher=q["encryption"] ?: "none",
                fingerprint=q["fp"] ?: q["fingerprint"],
                alpn=q["alpn"]?.split(",")
                    ?.map{it.trim()}
                    ?: emptyList(),
                allowInsecure=q["allowInsecure"]
                    .equals("1") ||
                    q["allowInsecure"]
                    .equals("true",true),
                realityPublicKey=q["pbk"],
                realityShortId=q["sid"],
                realitySpiderX=q["spx"]
            )
        )
    }
