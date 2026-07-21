package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals

class AarRecordEncoderTest {

    @Test
    fun `encodes external type record with android_com colon pkg type`() {
        val record = AarRecordEncoder.encode("com.example.myapp")
        assertEquals(Tnf.EXTERNAL_TYPE, record.tnf)
        assertEquals("android.com:pkg", String(record.type, Charsets.US_ASCII))
        assertEquals("com.example.myapp", String(record.payload, Charsets.UTF_8))
    }

    @Test
    fun `round-trips through NdefParser as second record after a primary payload`() {
        val uriRecord = UriRecordEncoder.encode("https://example.com")
        val aarRecord = AarRecordEncoder.encode("com.example.myapp")
        val message = NdefMessageEncoder.encode(listOf(uriRecord, aarRecord))

        val parsed = NdefParser.parse(message)
        assertEquals(2, parsed.size)
        assertEquals(Tnf.EXTERNAL_TYPE, parsed[1].tnf)
        assertEquals("com.example.myapp", String(parsed[1].payload, Charsets.UTF_8))
        assert(parsed[1].messageEnd)
        assert(!parsed[0].messageEnd)
    }
}
