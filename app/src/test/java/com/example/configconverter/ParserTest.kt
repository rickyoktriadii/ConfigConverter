package com.example.configconverter

import org.junit.Assert.*
import org.junit.Test

class ParserTest {
    private val vmess = "vmess://eyJ2IjoiMiIsInBzIjoidHJpYWw5MDcyIiwiYWRkIjoibnVzYS5jaG9zdG9yZS5iaXouaWQiLCJwb3J0IjoiNDQzIiwiaWQiOiIyYzdjOGY1My1mNDUyLTQ5YWUtOTE2MC1kYmUwODg0ZDFkYzEiLCJhaWQiOiIwIiwibmV0Ijoid3MiLCJ0eXBlIjoibm9uZSIsImhvc3QiOiJudXNhLmNob3N0b3JlLmJpei5pZCIsInBhdGgiOiIvdm1lc3MiLCJ0bHMiOiJ0bHMiLCJzbmkiOiJudXNhLmNob3N0b3JlLmJpei5pZCIsImFsbG93SW5zZWN1cmUiOiIxIiwiYWxwbiI6Imh0dHAvMS4xIn0="

    @Test fun parsesVmess() {
        val r = ConfigParser.parse(vmess)
        assertTrue(r is ParseResult.Ok)
        val c = (r as ParseResult.Ok).config
        assertEquals("trial9072", c.name)
        assertEquals("nusa.chostore.biz.id", c.server)
        assertEquals(443, c.port)
        assertEquals("ws", c.network)
        assertTrue(c.tls)
        assertEquals("/vmess", c.path)
    }

    @Test fun rejectsInvalidBase64() {
        assertTrue(ConfigParser.parse("vmess://%%%") is ParseResult.Error)
    }

    @Test fun parsesVless() {
        val r = ConfigParser.parse("vless://550e8400-e29b-41d4-a716-446655440000@example.com:443?encryption=none&security=tls&type=ws&host=example.com&path=%2Fvless&sni=example.com#My-VLESS")
        assertTrue(r is ParseResult.Ok)
        val c = (r as ParseResult.Ok).config
        assertEquals("vless", c.protocol)
        assertEquals("/vless", c.path)
        assertTrue(c.tls)
    }

    @Test fun parsesTrojan() {
        val r = ConfigParser.parse("trojan://secret%40pass@example.com:443?security=tls&sni=example.com&type=grpc&serviceName=demo#Trojan")
        assertTrue(r is ParseResult.Ok)
        val c = (r as ParseResult.Ok).config
        assertEquals("trojan", c.protocol)
        assertEquals("secret@pass", c.password)
        assertEquals("demo", c.serviceName)
    }
}
