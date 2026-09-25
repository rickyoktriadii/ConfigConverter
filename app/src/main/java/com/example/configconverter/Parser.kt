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
        parseXray(s)

    s.startsWith("vmess://", true) ->
        parseVmess(s)

    s.startsWith("vless://", true) ->
        parseVless(s)

    s.startsWith("trojan://", true) ->
        parseTrojan(s)

    s.startsWith("ss://", true) ->
        parseShadowsocks(s)

    s.startsWith("socks://", true) ||
    s.startsWith("socks5://", true) ->
        parseSocks(s)

    s.startsWith("http://", true) ||
    s.startsWith("https://", true) ->
        parseHttp(s)

    s.startsWith("wireguard://", true) ->
        parseWireGuard(s)

    s.startsWith("hysteria2://", true) ||
    s.startsWith("hy2://", true) ||
    s.startsWith("hysteria://", true) ->
        parseHysteria(s)

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

        private fun ss(s: String): ParseResult {
        val x = s.substringAfter("://")
        val name = x.substringAfter("#","").let(::dec)
        val body = x.substringBefore("#")

        val decoded = if (body.contains("@"))
            body
        else
            b64(body)
                ?: return ParseResult.Error("Invalid Shadowsocks")

        val auth = decoded.substringBeforeLast("@")
        val ep = decoded.substringAfterLast("@")

        val method = auth.substringBefore(":")
        val pass = auth.substringAfter(":", "")

        val host = ep.substringBeforeLast(":")
            .trim('[',']')

        val p = ep.substringAfterLast(":")
            .toIntOrNull()
            ?: return ParseResult.Error("Invalid Shadowsocks port")

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="shadowsocks",
                name=name,
                server=host,
                port=p,
                password=pass,
                method=method,
                cipher=method
            )
        )
    }

    private fun socks(s: String): ParseResult {
        val u = runCatching { URI(s) }.getOrElse {
            return ParseResult.Error("Invalid SOCKS URI")
        }

        val host = u.host
            ?: return ParseResult.Error("SOCKS server missing")

        val ui = u.rawUserInfo

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="socks",
                name=u.rawFragment?.let(::dec) ?: "",
                server=host,
                port=port(u,1080),
                username=ui?.substringBefore(":")?.let(::dec),
                password=ui?.substringAfter(":","")?.let(::dec),
                udp=true
            )
        )
    }

    private fun http(s: String): ParseResult {
        val u = runCatching { URI(s) }.getOrElse {
            return ParseResult.Error("Invalid HTTP URI")
        }

        val host = u.host
            ?: return ParseResult.Error("HTTP server missing")

        val ui = u.rawUserInfo

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="http",
                name=u.rawFragment?.let(::dec) ?: "",
                server=host,
                port=port(u,8080),
                username=ui?.substringBefore(":")?.let(::dec),
                password=ui?.substringAfter(":","")?.let(::dec)
            )
        )
    }

        private fun wireguard(s: String): ParseResult {
        val u = runCatching { URI(s) }.getOrElse {
            return ParseResult.Error("Invalid WireGuard URI")
        }

        val q = query(u)

        val host = u.host
            ?: return ParseResult.Error("WireGuard server missing")

        val addresses =
            q["address"]
                ?.split(",")
                ?.map{it.trim()}
                ?.filter{it.isNotBlank()}
                ?: emptyList()

        val allowed =
            (q["allowedips"] ?: "0.0.0.0/0,::/0")
                .split(",")
                .map{it.trim()}
                .filter{it.isNotBlank()}

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="wireguard",
                name=u.rawFragment?.let(::dec) ?: "",
                server=host,
                port=port(u,51820),
                privateKey=u.rawUserInfo?.let(::dec)
                    ?: q["privatekey"],
                peerPublicKey=q["publickey"]
                    ?: q["peerPublicKey"],
                publicKey=q["publickey"],
                allowedIPs=allowed,
                localAddress=addresses,
                mtu=q["mtu"]?.toIntOrNull()
            )
        )
    }

    private fun hysteria(s: String): ParseResult {
        val u = runCatching { URI(s) }.getOrElse {
            return ParseResult.Error("Invalid Hysteria URI")
        }

        val q = query(u)

        val host = u.host
            ?: return ParseResult.Error("Hysteria server missing")

        return ParseResult.Ok(
            ConnectionConfig(
                protocol="hysteria",
                name=u.rawFragment?.let(::dec) ?: "",
                server=host,
                port=port(u),
                password=u.rawUserInfo?.let(::dec)
                    ?: q["auth"],
                network="hysteria",
                security="tls",
                tls=true,
                sni=q["sni"] ?: host,
                alpn=q["alpn"]?.split(",")
                    ?.map{it.trim()}
                    ?: listOf("h3"),
                allowInsecure=q["insecure"]
                    .equals("1") ||
                    q["insecure"]
                    .equals("true",true),
                obfs=q["obfs"],
                obfsPassword=q["obfs-password"]
                    ?: q["obfs_password"],
                upMbps=q["upmbps"]?.toIntOrNull(),
                downMbps=q["downmbps"]?.toIntOrNull()
            )
        )
    }

        private fun xray(s: String): ParseResult {
        val root = runCatching {
            Json.parseToJsonElement(s).jsonObject
        }.getOrElse {
            return ParseResult.Error("Invalid Xray JSON")
        }

        val out = root["outbounds"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?: return ParseResult.Error("Outbound not found")

        val protocol = out["protocol"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.lowercase()
            ?: return ParseResult.Error("Protocol missing")

        val set = out["settings"]?.jsonObject
            ?: buildJsonObject {}

        val stream = out["streamSettings"]?.jsonObject
            ?: buildJsonObject {}

        val method =
            stream["method"]?.jsonPrimitive?.contentOrNull
            ?: stream["network"]?.jsonPrimitive?.contentOrNull
            ?: "raw"

        val vnext = set["vnext"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject

        val user = vnext
            ?.get("users")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject

        val server =
            set["address"]?.jsonPrimitive?.contentOrNull
                ?: vnext?.get("address")
                    ?.jsonPrimitive?.contentOrNull
                ?: return ParseResult.Error("Server missing")

        val p =
            set["port"]?.jsonPrimitive?.intOrNull
                ?: vnext?.get("port")
                    ?.jsonPrimitive?.intOrNull
                ?: return ParseResult.Error("Port missing")

        val tls = stream["tlsSettings"]?.jsonObject
        val reality = stream["realitySettings"]?.jsonObject
        val ws = stream["wsSettings"]?.jsonObject
        val grpc = stream["grpcSettings"]?.jsonObject

        val sni =
            tls?.get("serverName")?.jsonPrimitive?.contentOrNull
                ?: reality?.get("serverName")
                    ?.jsonPrimitive?.contentOrNull

        val fp =
            tls?.get("fingerprint")?.jsonPrimitive?.contentOrNull
                ?: reality?.get("fingerprint")
                    ?.jsonPrimitive?.contentOrNull

        return ParseResult.Ok(
            ConnectionConfig(
                protocol=protocol,
                name=out["tag"]?.jsonPrimitive?.contentOrNull ?: "",
                server=server,
                port=p,
                uuid=set["id"]?.jsonPrimitive?.contentOrNull
                    ?: user?.get("id")?.jsonPrimitive?.contentOrNull,
                password=set["password"]?.jsonPrimitive?.contentOrNull,
                alterId=user?.get("alterId")
                    ?.jsonPrimitive?.intOrNull ?: 0,
                cipher=set["security"]?.jsonPrimitive?.contentOrNull
                    ?: user?.get("security")
                        ?.jsonPrimitive?.contentOrNull
                    ?: user?.get("encryption")
                        ?.jsonPrimitive?.contentOrNull
                    ?: "none",
                network=method,
                security=stream["security"]
                    ?.jsonPrimitive?.contentOrNull,
                tls=stream["security"]
                    ?.jsonPrimitive?.contentOrNull
                    ?.equals("none",true) == false,
                sni=sni,
                host=ws?.get("headers")
                    ?.jsonObject?.get("Host")
                    ?.jsonPrimitive?.contentOrNull,
                path=ws?.get("path")
                    ?.jsonPrimitive?.contentOrNull,
                serviceName=grpc?.get("serviceName")
                    ?.jsonPrimitive?.contentOrNull,
                flow=set["flow"]?.jsonPrimitive?.contentOrNull
                    ?: user?.get("flow")
                        ?.jsonPrimitive?.contentOrNull,
                fingerprint=fp,
                alpn=tls?.get("alpn")?.jsonArray
                    ?.mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    } ?: emptyList(),
                allowInsecure=tls?.get("allowInsecure")
                    ?.jsonPrimitive?.booleanOrNull ?: false,
                realityPublicKey=reality?.get("password")
                    ?.jsonPrimitive?.contentOrNull,
                realityShortId=reality?.get("shortId")
                    ?.jsonPrimitive?.contentOrNull,
                realitySpiderX=reality?.get("spiderX")
                    ?.jsonPrimitive?.contentOrNull
            )
        )
    }
}
