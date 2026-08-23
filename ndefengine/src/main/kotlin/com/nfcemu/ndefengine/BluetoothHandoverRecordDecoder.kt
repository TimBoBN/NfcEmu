package com.nfcemu.ndefengine

/**
 * Inverse of [BluetoothHandoverRecordEncoder]'s OOB data block - also the format used by
 * real-world "tap to pair" Bluetooth accessory tags, so this is written to tolerate EIR
 * blocks this app never emits itself (Class of Device, Service UUIDs, ...): unknown block
 * types are skipped rather than aborting the parse. A shape this doesn't recognize at all
 * degrades to `null` (the caller surfaces that as "unsupported") rather than throwing.
 */
object BluetoothHandoverRecordDecoder {

    private const val AD_TYPE_SHORTENED_LOCAL_NAME = 0x08
    private const val AD_TYPE_COMPLETE_LOCAL_NAME = 0x09
    private const val ADDRESS_SIZE = 6
    private const val HEADER_SIZE = 2 + ADDRESS_SIZE // length field + device address

    /** [oobCarrierPayload] is the MIME carrier record's payload, i.e. the OOB data block. */
    fun decode(oobCarrierPayload: ByteArray): NdefPayload.BluetoothHandover? {
        if (oobCarrierPayload.size < HEADER_SIZE) return null
        val length = (oobCarrierPayload[0].toInt() and 0xFF) or ((oobCarrierPayload[1].toInt() and 0xFF) shl 8)
        if (length < HEADER_SIZE || length > oobCarrierPayload.size) return null

        val addressBytesLe = oobCarrierPayload.copyOfRange(2, HEADER_SIZE)
        val address = addressBytesLe.reversedArray().joinToString(":") { "%02X".format(it) }

        var shortName: String? = null
        var completeName: String? = null
        var offset = HEADER_SIZE
        while (offset < length) {
            val blockLength = oobCarrierPayload[offset].toInt() and 0xFF
            if (blockLength == 0 || offset + 1 + blockLength > length) break
            val adType = oobCarrierPayload[offset + 1].toInt() and 0xFF
            val dataStart = offset + 2
            val dataEnd = offset + 1 + blockLength
            when (adType) {
                AD_TYPE_COMPLETE_LOCAL_NAME -> completeName = String(oobCarrierPayload, dataStart, dataEnd - dataStart, Charsets.UTF_8)
                AD_TYPE_SHORTENED_LOCAL_NAME -> shortName = String(oobCarrierPayload, dataStart, dataEnd - dataStart, Charsets.UTF_8)
            }
            offset = dataEnd
        }

        return NdefPayload.BluetoothHandover(deviceAddress = address, deviceName = completeName ?: shortName)
    }
}
