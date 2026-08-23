package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BluetoothHandoverRecordEncoderTest {

    @Test
    fun `produces exactly two records - Hs select and bluetooth oob carrier`() {
        val records = BluetoothHandoverRecordEncoder.encode(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF", "Speaker"))
        assertEquals(2, records.size)
        assertEquals(Tnf.WELL_KNOWN, records[0].tnf)
        assertEquals("Hs", String(records[0].type, Charsets.US_ASCII))
        assertEquals(Tnf.MIME_MEDIA, records[1].tnf)
        assertEquals("application/vnd.bluetooth.ep.oob", String(records[1].type, Charsets.US_ASCII))
        assertEquals("0", String(records[1].id, Charsets.US_ASCII))
    }

    @Test
    fun `device address is stored least-significant-byte first per the oob spec`() {
        val records = BluetoothHandoverRecordEncoder.encode(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF"))
        val payload = records[1].payload
        // bytes 2..7 = address, reversed relative to the canonical "AA:BB:CC:DD:EE:FF" display order
        val addressBytes = payload.copyOfRange(2, 8)
        assertEquals(listOf(0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA), addressBytes.map { it.toInt() and 0xFF })
    }

    @Test
    fun `an invalid address format is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            BluetoothHandoverRecordEncoder.encode(NdefPayload.BluetoothHandover("not-a-mac"))
        }
    }

    @Test
    fun `whole message round-trips through the message factory and decoder`() {
        val bytes = NdefMessageFactory.build(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF", "Acme Speaker"))
        val result = NdefPayloadDecoder.decode(bytes)
        val success = result as? DecodedTagResult.Success
        assertTrue(success != null)
        assertEquals(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF", "Acme Speaker"), success.payload)
    }

    @Test
    fun `no device name omits the eir name block entirely`() {
        val records = BluetoothHandoverRecordEncoder.encode(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF"))
        val payload = records[1].payload
        assertEquals(8, payload.size) // 2-byte length + 6-byte address, no EIR blocks
        val decoded = BluetoothHandoverRecordDecoder.decode(payload)
        assertNull(decoded?.deviceName)
    }
}
