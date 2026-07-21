package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NdefMessageEncoderTest {

    @Test
    fun `single short record sets MB, ME and SR, clears CF and IL`() {
        val record = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = byteArrayOf('T'.code.toByte()), payload = byteArrayOf(1, 2, 3))
        val bytes = NdefMessageEncoder.encode(listOf(record))

        val header = bytes[0].toInt() and 0xFF
        assertEquals(0x80, header and 0x80, "MB must be set")
        assertEquals(0x40, header and 0x40, "ME must be set")
        assertEquals(0x00, header and 0x20, "CF must be clear")
        assertEquals(0x10, header and 0x10, "SR must be set for payload <= 255 bytes")
        assertEquals(0x00, header and 0x08, "IL must be clear when no id present")
        assertEquals(Tnf.WELL_KNOWN, header and 0x07)

        assertEquals(1, bytes[1].toInt(), "type length")
        assertEquals(3, bytes[2].toInt(), "short payload length")
        assertEquals('T'.code.toByte(), bytes[3])
        assertEquals(listOf<Byte>(1, 2, 3), bytes.drop(4))
    }

    @Test
    fun `payload over 255 bytes uses long 4-byte length and clears SR`() {
        val payload = ByteArray(300) { it.toByte() }
        val record = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = byteArrayOf('T'.code.toByte()), payload = payload)
        val bytes = NdefMessageEncoder.encode(listOf(record))

        val header = bytes[0].toInt() and 0xFF
        assertEquals(0x00, header and 0x10, "SR must be clear for payload > 255 bytes")

        val length = ((bytes[2].toInt() and 0xFF) shl 24) or
            ((bytes[3].toInt() and 0xFF) shl 16) or
            ((bytes[4].toInt() and 0xFF) shl 8) or
            (bytes[5].toInt() and 0xFF)
        assertEquals(300, length)

        val parsed = NdefParser.parse(bytes)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].payload.contentEquals(payload))
    }

    @Test
    fun `id present sets IL and emits id length and bytes`() {
        val record = RawNdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "application/vnd.wfa.wsc".toByteArray(),
            id = "0".toByteArray(),
            payload = byteArrayOf(9, 9),
        )
        val bytes = NdefMessageEncoder.encode(listOf(record))
        val header = bytes[0].toInt() and 0xFF
        assertEquals(0x08, header and 0x08, "IL must be set")

        val parsed = NdefParser.parse(bytes).single()
        assertTrue(parsed.id.contentEquals("0".toByteArray()))
    }

    @Test
    fun `multiple records set MB only on first and ME only on last`() {
        val a = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = byteArrayOf('T'.code.toByte()), payload = byteArrayOf(1))
        val b = RawNdefRecord(tnf = Tnf.EXTERNAL_TYPE, type = "android.com:pkg".toByteArray(), payload = "com.example".toByteArray())
        val bytes = NdefMessageEncoder.encode(listOf(a, b))

        val parsed = NdefParser.parse(bytes)
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].messageBegin)
        assertTrue(!parsed[0].messageEnd)
        assertTrue(!parsed[1].messageBegin)
        assertTrue(parsed[1].messageEnd)
    }

    @Test
    fun `empty record list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            NdefMessageEncoder.encode(emptyList())
        }
    }

    @Test
    fun `empty payload is encoded without crashing`() {
        val record = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = byteArrayOf('T'.code.toByte()), payload = ByteArray(0))
        val bytes = NdefMessageEncoder.encode(listOf(record))
        val parsed = NdefParser.parse(bytes).single()
        assertEquals(0, parsed.payload.size)
    }
}
