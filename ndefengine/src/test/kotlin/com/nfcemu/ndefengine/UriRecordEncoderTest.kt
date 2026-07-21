package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals

class UriRecordEncoderTest {

    private fun decode(uri: String): Pair<Int, String> {
        val record = UriRecordEncoder.encode(uri)
        val code = record.payload[0].toInt() and 0xFF
        val remainder = String(record.payload, 1, record.payload.size - 1, Charsets.UTF_8)
        return code to remainder
    }

    @Test
    fun `https with www uses code 0x02`() {
        val (code, remainder) = decode("https://www.example.com")
        assertEquals(0x02, code)
        assertEquals("example.com", remainder)
    }

    @Test
    fun `https without www uses code 0x04, not the shorter http match`() {
        val (code, remainder) = decode("https://example.com/path")
        assertEquals(0x04, code)
        assertEquals("example.com/path", remainder)
    }

    @Test
    fun `tel uri uses code 0x05`() {
        val (code, remainder) = decode("tel:+491701234567")
        assertEquals(0x05, code)
        assertEquals("+491701234567", remainder)
    }

    @Test
    fun `mailto uri uses code 0x06 and keeps query string for subject and body`() {
        val (code, remainder) = decode("mailto:test@example.com?subject=Hi&body=Hello")
        assertEquals(0x06, code)
        assertEquals("test@example.com?subject=Hi&body=Hello", remainder)
    }

    @Test
    fun `unmatched custom scheme falls back to code 0x00 with full uri preserved`() {
        val (code, remainder) = decode("market://details?id=com.example.app")
        assertEquals(0x00, code)
        assertEquals("market://details?id=com.example.app", remainder)
    }

    @Test
    fun `custom deep link scheme falls back to code 0x00`() {
        val (code, remainder) = decode("myapp://open/screen?x=1")
        assertEquals(0x00, code)
        assertEquals("myapp://open/screen?x=1", remainder)
    }

    @Test
    fun `geo uri falls back to code 0x00 since geo has no dedicated prefix code`() {
        val (code, remainder) = decode("geo:52.5200,13.4050")
        assertEquals(0x00, code)
        assertEquals("geo:52.5200,13.4050", remainder)
    }

    @Test
    fun `record type is well-known U`() {
        val record = UriRecordEncoder.encode("https://example.com")
        assertEquals(Tnf.WELL_KNOWN, record.tnf)
        assertEquals('U'.code.toByte(), record.type[0])
    }

    @Test
    fun `utf-8 characters in path round-trip`() {
        val uri = "https://example.com/müller?straße=café"
        val (_, remainder) = decode(uri)
        assertEquals(uri.removePrefix("https://"), remainder)
    }
}
