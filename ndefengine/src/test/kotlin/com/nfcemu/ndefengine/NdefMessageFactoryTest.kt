package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NdefMessageFactoryTest {

    @Test
    fun `uri payload without aar produces a single message-begin-and-end record`() {
        val bytes = NdefMessageFactory.build(NdefPayload.Uri("https://example.com"))
        val parsed = NdefParser.parse(bytes)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].messageBegin && parsed[0].messageEnd)
        assertEquals(Tnf.WELL_KNOWN, parsed[0].tnf)
    }

    @Test
    fun `aar is appended as final record and does not disturb the primary payload`() {
        val bytes = NdefMessageFactory.build(
            NdefPayload.Text("Hallo"),
            aar = AarConfig("com.example.myapp"),
        )
        val parsed = NdefParser.parse(bytes)
        assertEquals(2, parsed.size)
        assertEquals(Tnf.WELL_KNOWN, parsed[0].tnf)
        assertEquals("T", String(parsed[0].type, Charsets.US_ASCII))
        assertEquals(Tnf.EXTERNAL_TYPE, parsed[1].tnf)
        assertEquals("com.example.myapp", String(parsed[1].payload, Charsets.UTF_8))
        assertTrue(parsed[0].messageBegin && !parsed[0].messageEnd)
        assertTrue(!parsed[1].messageBegin && parsed[1].messageEnd)
    }

    @Test
    fun `wifi handover plus aar produces three records with aar last`() {
        val bytes = NdefMessageFactory.build(
            NdefPayload.WifiHandover("Net", WifiAuthType.WPA2_PSK, "password1"),
            aar = AarConfig("com.example.myapp"),
        )
        val parsed = NdefParser.parse(bytes)
        assertEquals(3, parsed.size)
        assertEquals(Tnf.EXTERNAL_TYPE, parsed[2].tnf)
        assertTrue(parsed[2].messageEnd)
    }

    @Test
    fun `large vcard forces long-form 4-byte length and still round-trips (chunking precondition)`() {
        val longAddress = "A".repeat(400)
        val bytes = NdefMessageFactory.build(NdefPayload.VCard(name = "Big Person", address = longAddress))
        assertTrue(bytes.size > 256, "message must exceed a single APDU READ BINARY response to be a meaningful chunking test")

        val parsed = NdefParser.parse(bytes).single()
        val body = String(parsed.payload, Charsets.UTF_8)
        assertTrue(body.contains(longAddress))
    }

    @Test
    fun `every supported payload type round-trips without throwing`() {
        val payloads = listOf(
            NdefPayload.Uri("tel:+491701234567"),
            NdefPayload.Uri("mailto:test@example.com?subject=Hi"),
            NdefPayload.Uri("sms:+491701234567?body=Hallo"),
            NdefPayload.Uri("geo:52.5200,13.4050"),
            NdefPayload.Uri("market://details?id=com.example.app"),
            NdefPayload.VCard(name = "Edge Case", phones = emptyList(), emails = emptyList()),
            NdefPayload.Text(""),
            NdefPayload.Text("emoji test 😀🚀", languageCode = "en"),
            NdefPayload.WifiHandover("Empty", WifiAuthType.OPEN),
            NdefPayload.WifiHandover("Wep", WifiAuthType.WEP, "12345"),
        )
        for (payload in payloads) {
            val bytes = NdefMessageFactory.build(payload)
            val parsed = NdefParser.parse(bytes)
            assertTrue(parsed.isNotEmpty())
        }
    }

    @Test
    fun `capability container max ndef size matches actual factory output length`() {
        val bytes = NdefMessageFactory.build(NdefPayload.Uri("https://example.com/very/long/path/segment/for/measurement"))
        val cc = CapabilityContainer.build(bytes.size)
        val maxSize = ((cc[11].toInt() and 0xFF) shl 8) or (cc[12].toInt() and 0xFF)
        assertEquals(bytes.size + 2, maxSize)
    }
}
