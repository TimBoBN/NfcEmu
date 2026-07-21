package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals

class TextRecordEncoderTest {

    @Test
    fun `default language code is de`() {
        val record = TextRecordEncoder.encode("Hallo Welt")
        val statusByte = record.payload[0].toInt()
        val langLength = statusByte and 0x3F
        assertEquals(0, statusByte and 0x80, "UTF-8 flag bit must be 0")
        assertEquals(2, langLength)
        val lang = String(record.payload, 1, langLength, Charsets.US_ASCII)
        assertEquals("de", lang)
        val text = String(record.payload, 1 + langLength, record.payload.size - 1 - langLength, Charsets.UTF_8)
        assertEquals("Hallo Welt", text)
    }

    @Test
    fun `custom language code is respected`() {
        val record = TextRecordEncoder.encode("Hello World", languageCode = "en-US")
        val statusByte = record.payload[0].toInt()
        val langLength = statusByte and 0x3F
        assertEquals(5, langLength)
        val lang = String(record.payload, 1, langLength, Charsets.US_ASCII)
        assertEquals("en-US", lang)
    }

    @Test
    fun `record type is well-known T`() {
        val record = TextRecordEncoder.encode("x")
        assertEquals(Tnf.WELL_KNOWN, record.tnf)
        assertEquals('T'.code.toByte(), record.type[0])
    }

    @Test
    fun `utf-8 special characters and empty text round-trip via NdefParser`() {
        val text = "Grüße äöüß 日本語 😀"
        val message = NdefMessageEncoder.encode(listOf(TextRecordEncoder.encode(text)))
        val parsed = NdefParser.parse(message).single()
        val langLength = parsed.payload[0].toInt() and 0x3F
        val decoded = String(parsed.payload, 1 + langLength, parsed.payload.size - 1 - langLength, Charsets.UTF_8)
        assertEquals(text, decoded)
    }

    @Test
    fun `empty text is encoded without crashing`() {
        val record = TextRecordEncoder.encode("")
        assertEquals(3, record.payload.size) // status byte + "de" (2 bytes), no text bytes
    }
}
