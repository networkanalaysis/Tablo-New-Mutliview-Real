package com.example

import com.example.network.TabloHmac
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabloHmacTest {

    @Test
    fun testTabloHmacSigning() {
        val (authHeader, dateHeader) = TabloHmac.makeDeviceAuth(
            method = "GET",
            path = "/guide/channels"
        )

        assertNotNull(authHeader)
        assertNotNull(dateHeader)
        assertTrue(authHeader.startsWith("tablo:"))
        assertTrue(dateHeader.endsWith("GMT"))
    }

    @Test
    fun testTabloHmacWithBody() {
        val (authHeader, dateHeader) = TabloHmac.makeDeviceAuth(
            method = "POST",
            path = "/guide/channels/101/watch",
            body = "{\"client_id\":\"test-client-123\"}"
        )

        assertNotNull(authHeader)
        assertTrue(authHeader.startsWith("tablo:"))
        assertTrue(authHeader.contains(":"))
    }
}
