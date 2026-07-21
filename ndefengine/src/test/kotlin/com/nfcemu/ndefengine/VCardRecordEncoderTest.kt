package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VCardRecordEncoderTest {

    private fun bodyOf(vcard: NdefPayload.VCard): String =
        String(VCardRecordEncoder.encode(vcard).payload, Charsets.UTF_8)

    @Test
    fun `minimal vcard with all fields absent still produces a valid begin-end block`() {
        val body = bodyOf(NdefPayload.VCard())
        assertTrue(body.startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        assertTrue(body.endsWith("END:VCARD\r\n"))
        assertFalse(body.contains("FN:"))
        assertFalse(body.contains("TEL"))
    }

    @Test
    fun `blank strings and blank list entries are omitted, not emitted empty`() {
        val body = bodyOf(NdefPayload.VCard(name = "  ", phones = listOf("", "  "), emails = listOf("")))
        assertFalse(body.contains("FN:"))
        assertFalse(body.contains("TEL"))
        assertFalse(body.contains("EMAIL"))
    }

    @Test
    fun `all fields populated appear with correct vcard property names`() {
        val vcard = NdefPayload.VCard(
            name = "Ada Lovelace",
            phones = listOf("+491701234567", "+491709998877"),
            emails = listOf("ada@example.com"),
            organization = "Analytical Engines Inc.",
            title = "Chief Mathematician",
            website = "https://example.com",
            address = "Main Street 1, 12345 Berlin",
        )
        val body = bodyOf(vcard)
        assertTrue(body.contains("FN:Ada Lovelace\r\n"))
        assertTrue(body.contains("N:Ada Lovelace;;;;\r\n"))
        assertTrue(body.contains("TEL;TYPE=CELL:+491701234567\r\n"))
        assertTrue(body.contains("TEL;TYPE=CELL:+491709998877\r\n"))
        assertTrue(body.contains("EMAIL:ada@example.com\r\n"))
        assertTrue(body.contains("ORG:Analytical Engines Inc.\r\n"))
        assertTrue(body.contains("TITLE:Chief Mathematician\r\n"))
        assertTrue(body.contains("URL:https://example.com\r\n"))
        assertTrue(body.contains("ADR:;;Main Street 1\\, 12345 Berlin;;;;\r\n"))
    }

    @Test
    fun `special characters are escaped per rfc 2426`() {
        val vcard = NdefPayload.VCard(name = "Müller; Jörg, \"Junior\"\nGmbH")
        val body = bodyOf(vcard)
        assertTrue(body.contains("FN:Müller\\; Jörg\\, \"Junior\"\\nGmbH\r\n"))
    }

    @Test
    fun `record uses MIME media tnf and text-vcard type`() {
        val record = VCardRecordEncoder.encode(NdefPayload.VCard(name = "X"))
        assertEquals(Tnf.MIME_MEDIA, record.tnf)
        assertEquals("text/vcard", String(record.type, Charsets.US_ASCII))
    }

    @Test
    fun `round-trips through NdefParser`() {
        val vcard = NdefPayload.VCard(name = "Test User", phones = listOf("123"))
        val message = NdefMessageEncoder.encode(listOf(VCardRecordEncoder.encode(vcard)))
        val parsed = NdefParser.parse(message).single()
        assertEquals("text/vcard", String(parsed.type, Charsets.US_ASCII))
        assertTrue(String(parsed.payload, Charsets.UTF_8).contains("FN:Test User"))
    }
}
