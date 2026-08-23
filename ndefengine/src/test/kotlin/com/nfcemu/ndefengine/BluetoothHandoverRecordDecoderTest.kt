package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BluetoothHandoverRecordDecoderTest {

    /**
     * Real "tap to pair" Bluetooth accessory tags aren't produced by this app's own encoder -
     * they typically carry extra EIR blocks (Class of Device, Service UUIDs, ...) this app has
     * no use for, before or after the name block. Those must be skipped rather than aborting.
     */
    @Test
    fun `a foreign oob payload with unrelated eir blocks in between still yields address and name`() {
        val addressLsbFirst = byteArrayOf(0xFF.toByte(), 0xEE.toByte(), 0xDD.toByte(), 0xCC.toByte(), 0xBB.toByte(), 0xAA.toByte())
        val classOfDeviceBlock = byteArrayOf(4, 0x0D, 0x04, 0x04, 0x24) // [len][type=CoD][3 bytes of data]
        val nameBytes = "Acme Speaker".toByteArray(Charsets.UTF_8)
        val nameBlock = byteArrayOf((nameBytes.size + 1).toByte(), 0x09) + nameBytes // type=Complete Local Name

        val body = addressLsbFirst + classOfDeviceBlock + nameBlock
        val totalLength = 2 + body.size
        val lengthLe = byteArrayOf((totalLength and 0xFF).toByte(), ((totalLength shr 8) and 0xFF).toByte())
        val payload = lengthLe + body

        val decoded = BluetoothHandoverRecordDecoder.decode(payload)
        assertEquals(NdefPayload.BluetoothHandover("AA:BB:CC:DD:EE:FF", "Acme Speaker"), decoded)
    }

    @Test
    fun `a shortened local name is used when no complete name block is present`() {
        val addressLsbFirst = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val nameBytes = "Spk".toByteArray(Charsets.UTF_8)
        val shortNameBlock = byteArrayOf((nameBytes.size + 1).toByte(), 0x08) + nameBytes // type=Shortened Local Name

        val body = addressLsbFirst + shortNameBlock
        val totalLength = 2 + body.size
        val lengthLe = byteArrayOf((totalLength and 0xFF).toByte(), ((totalLength shr 8) and 0xFF).toByte())
        val payload = lengthLe + body

        val decoded = BluetoothHandoverRecordDecoder.decode(payload)
        assertEquals(NdefPayload.BluetoothHandover("06:05:04:03:02:01", "Spk"), decoded)
    }

    @Test
    fun `a payload shorter than the minimum header is unsupported rather than throwing`() {
        assertNull(BluetoothHandoverRecordDecoder.decode(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `an address with no eir blocks at all decodes with a null name`() {
        val addressLsbFirst = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val totalLength = 2 + addressLsbFirst.size
        val lengthLe = byteArrayOf((totalLength and 0xFF).toByte(), ((totalLength shr 8) and 0xFF).toByte())
        val payload = lengthLe + addressLsbFirst

        val decoded = BluetoothHandoverRecordDecoder.decode(payload)
        assertEquals(NdefPayload.BluetoothHandover("06:05:04:03:02:01", null), decoded)
    }
}
