package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WifiHandoverRecordEncoderTest {

    /** Parses top-level WSC TLVs (attribute id: 2 bytes, length: 2 bytes, value: n bytes) into a map. */
    private fun parseTlvs(data: ByteArray): Map<Int, ByteArray> {
        val result = mutableMapOf<Int, ByteArray>()
        var offset = 0
        while (offset + 4 <= data.size) {
            val id = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val len = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            offset += 4
            result[id] = data.copyOfRange(offset, offset + len)
            offset += len
        }
        return result
    }

    @Test
    fun `produces exactly two records - Hs select and wsc carrier`() {
        val records = WifiHandoverRecordEncoder.encode(NdefPayload.WifiHandover("HomeNet", WifiAuthType.WPA2_PSK, "supersecret"))
        assertEquals(2, records.size)
        assertEquals(Tnf.WELL_KNOWN, records[0].tnf)
        assertEquals("Hs", String(records[0].type, Charsets.US_ASCII))
        assertEquals(Tnf.MIME_MEDIA, records[1].tnf)
        assertEquals("application/vnd.wfa.wsc", String(records[1].type, Charsets.US_ASCII))
        assertEquals("0", String(records[1].id, Charsets.US_ASCII))
    }

    @Test
    fun `hs record embeds an ac record referencing the same carrier id`() {
        val records = WifiHandoverRecordEncoder.encode(NdefPayload.WifiHandover("Net", WifiAuthType.OPEN))
        val hsPayload = records[0].payload
        assertEquals(0x12, hsPayload[0].toInt(), "version byte")
        val embedded = hsPayload.copyOfRange(1, hsPayload.size)
        val acRecords = NdefParser.parse(embedded)
        assertEquals(1, acRecords.size)
        assertEquals("ac", String(acRecords[0].type, Charsets.US_ASCII))
        val acPayload = acRecords[0].payload
        val refLength = acPayload[1].toInt() and 0xFF
        val ref = String(acPayload, 2, refLength, Charsets.US_ASCII)
        assertEquals("0", ref)
    }

    @Test
    fun `wpa2-psk credential contains ssid, auth type, encryption type and network key`() {
        val records = WifiHandoverRecordEncoder.encode(NdefPayload.WifiHandover("MyWifi", WifiAuthType.WPA2_PSK, "hunter2pass"))
        val credentialTlv = parseTlvs(records[1].payload)
        val credentialBody = credentialTlv.getValue(0x100E)
        val fields = parseTlvs(credentialBody)

        assertEquals("MyWifi", String(fields.getValue(0x1045), Charsets.UTF_8))
        assertEquals(0x0020, ((fields.getValue(0x1003)[0].toInt() and 0xFF) shl 8) or (fields.getValue(0x1003)[1].toInt() and 0xFF))
        assertEquals(0x0008, ((fields.getValue(0x100F)[0].toInt() and 0xFF) shl 8) or (fields.getValue(0x100F)[1].toInt() and 0xFF))
        assertEquals("hunter2pass", String(fields.getValue(0x1027), Charsets.UTF_8))
    }

    @Test
    fun `open network omits network key attribute entirely`() {
        val records = WifiHandoverRecordEncoder.encode(NdefPayload.WifiHandover("OpenNet", WifiAuthType.OPEN, password = null))
        val credentialBody = parseTlvs(records[1].payload).getValue(0x100E)
        val fields = parseTlvs(credentialBody)
        assertTrue(0x1027 !in fields, "no network key attribute expected for an open network")
    }

    @Test
    fun `whole message round-trips through the message factory`() {
        val bytes = NdefMessageFactory.build(NdefPayload.WifiHandover("RoundTripNet", WifiAuthType.WPA_PSK, "password123"))
        val parsed = NdefParser.parse(bytes)
        assertEquals(2, parsed.size)
        assertTrue(parsed[0].messageBegin)
        assertTrue(parsed[1].messageEnd)
    }
}
