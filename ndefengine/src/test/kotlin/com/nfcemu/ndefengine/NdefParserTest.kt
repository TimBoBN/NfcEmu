package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NdefParserTest {

    @Test
    fun `unchunked multi-record message parses every record in order`() {
        val bytes = NdefMessageEncoder.encode(
            listOf(
                TextRecordEncoder.encode("first"),
                UriRecordEncoder.encode("https://example.com"),
            ),
        )
        val records = NdefParser.parse(bytes)
        assertEquals(2, records.size)
        assertEquals("first", TextRecordDecoder.decode(records[0].payload).text)
        assertEquals("https://example.com", UriRecordDecoder.decode(records[1].payload))
    }

    @Test
    fun `a chunked record is reassembled into a single logical record`() {
        val original = TextRecordEncoder.encode("Hello, chunked world!")
        val bytes = encodeChunked(original, chunkSizes = listOf(5, 5, original.payload.size - 10))

        val records = NdefParser.parse(bytes)

        assertEquals(1, records.size)
        val record = records.single()
        assertEquals(Tnf.WELL_KNOWN, record.tnf)
        assertEquals("T", String(record.type, Charsets.US_ASCII))
        assertTrue(record.messageBegin)
        assertTrue(record.messageEnd)
        assertEquals("Hello, chunked world!", TextRecordDecoder.decode(record.payload).text)
    }

    @Test
    fun `a chunked record followed by a normal record parses both correctly`() {
        val chunked = TextRecordEncoder.encode("chunked")
        val chunkedBytes = encodeChunked(chunked, chunkSizes = listOf(3, chunked.payload.size - 3), isLastRecordInMessage = false)
        val trailing = UriRecordEncoder.encode("https://example.com")
        val trailingBytes = encodeRecord(trailing, mb = false, me = true)
        val bytes = chunkedBytes + trailingBytes

        val records = NdefParser.parse(bytes)

        assertEquals(2, records.size)
        assertEquals("chunked", TextRecordDecoder.decode(records[0].payload).text)
        assertTrue(records[0].messageBegin)
        assertEquals("https://example.com", UriRecordDecoder.decode(records[1].payload))
        assertTrue(records[1].messageEnd)
    }

    @Test
    fun `an unterminated chunk sequence throws instead of silently dropping data`() {
        val original = TextRecordEncoder.encode("incomplete")
        val firstChunkSize = 4
        val bytes = encodeChunked(original, chunkSizes = listOf(firstChunkSize, original.payload.size - firstChunkSize))
        // Keep only the first (CF=1) chunk - the sequence never reaches its CF=0 terminator.
        val firstChunkByteLength = 1 + 1 + 1 + original.type.size + firstChunkSize
        val truncated = bytes.copyOf(firstChunkByteLength)

        val error = assertFailsWith<IllegalArgumentException> { NdefParser.parse(truncated) }
        assertTrue(error.message.orEmpty().contains("never terminated"))
    }

    @Test
    fun `a continuation chunk with a non-UNCHANGED tnf is rejected as malformed`() {
        // Hand-craft: first chunk (CF=1) followed by a "continuation" that isn't TNF_UNCHANGED.
        val out = mutableListOf<Byte>()
        out.add((0x80 or 0x20 or 0x10 or Tnf.WELL_KNOWN).toByte()) // MB, CF, SR, TNF=WELL_KNOWN
        out.add(1) // type length
        out.add(2) // payload length
        out.add('T'.code.toByte())
        out.add(0); out.add('a'.code.toByte())
        // "Continuation" that wrongly claims TNF=WELL_KNOWN with a type field instead of UNCHANGED.
        out.add((0x40 or 0x10 or Tnf.WELL_KNOWN).toByte()) // ME, SR, TNF=WELL_KNOWN (invalid mid-chunk)
        out.add(1)
        out.add(1)
        out.add('U'.code.toByte())
        out.add('x'.code.toByte())

        assertFailsWith<IllegalArgumentException> { NdefParser.parse(out.toByteArray()) }
    }

    /**
     * Splits [record]'s payload into consecutive NDEF chunk records (CF=1 on every chunk but
     * the last), mirroring what a real chunking writer would produce - [NdefMessageEncoder]
     * deliberately never does this itself (see its kdoc), so tests exercising [NdefParser]'s
     * chunk-reassembly need their own encoder.
     */
    private fun encodeChunked(record: RawNdefRecord, chunkSizes: List<Int>, isLastRecordInMessage: Boolean = true): ByteArray {
        require(chunkSizes.sum() == record.payload.size)
        require(chunkSizes.size >= 2) { "at least 2 chunks needed to exercise chunking" }
        val out = mutableListOf<Byte>()
        var offset = 0
        chunkSizes.forEachIndexed { index, size ->
            val isFirst = index == 0
            val isLast = index == chunkSizes.lastIndex
            var header = 0
            if (isFirst) header = header or 0x80 // MB
            if (isLast && isLastRecordInMessage) header = header or 0x40 // ME
            if (!isLast) header = header or 0x20 // CF
            header = header or 0x10 // SR (all test chunks are short)
            header = header or (if (isFirst) record.tnf else Tnf.UNCHANGED)
            out.add(header.toByte())
            out.add((if (isFirst) record.type.size else 0).toByte())
            out.add(size.toByte())
            if (isFirst) record.type.forEach { out.add(it) }
            for (i in 0 until size) out.add(record.payload[offset + i])
            offset += size
        }
        return out.toByteArray()
    }

    /** Encodes a single, unchunked record with explicit MB/ME flags (independent of its position in a list). */
    private fun encodeRecord(record: RawNdefRecord, mb: Boolean, me: Boolean): ByteArray {
        val out = mutableListOf<Byte>()
        var header = 0
        if (mb) header = header or 0x80
        if (me) header = header or 0x40
        header = header or 0x10 // SR
        header = header or (record.tnf and 0x07)
        out.add(header.toByte())
        out.add(record.type.size.toByte())
        out.add(record.payload.size.toByte())
        record.type.forEach { out.add(it) }
        record.payload.forEach { out.add(it) }
        return out.toByteArray()
    }
}
