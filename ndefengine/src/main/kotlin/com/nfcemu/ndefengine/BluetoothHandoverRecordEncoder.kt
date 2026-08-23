package com.nfcemu.ndefengine

/**
 * Encodes Bluetooth pairing info as an NFC Forum Connection Handover "Handover Select"
 * message carrying a single Bluetooth OOB (Out-Of-Band) carrier, the same structure used
 * by "tap to pair" Bluetooth speaker/headset NFC tags:
 *
 * ```
 * NDEF message:
 *   [0] "Hs" record (Well Known)          - version byte + embedded message with one "ac" record
 *   [1] "application/vnd.bluetooth.ep.oob" - MIME record, id="0", payload = OOB data block
 * ```
 *
 * OOB payload layout (Bluetooth "Secure Simple Pairing Using NFC" spec):
 * `[length:2 LE][device address:6, LSB first][EIR data blocks...]`. Only the Complete Local
 * Name EIR block (type 0x09) is emitted when [NdefPayload.BluetoothHandover.deviceName] is set.
 */
object BluetoothHandoverRecordEncoder {

    private const val CARRIER_REF = "0"
    private const val AD_TYPE_COMPLETE_LOCAL_NAME = 0x09

    fun encode(bluetooth: NdefPayload.BluetoothHandover): List<RawNdefRecord> {
        val carrierRecord = RawNdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "application/vnd.bluetooth.ep.oob".toByteArray(Charsets.US_ASCII),
            id = CARRIER_REF.toByteArray(Charsets.US_ASCII),
            payload = buildOobPayload(bluetooth),
        )
        return ConnectionHandoverSupport.buildHandoverMessage(carrierRecord, CARRIER_REF)
    }

    private fun buildOobPayload(bluetooth: NdefPayload.BluetoothHandover): ByteArray {
        val addressBytes = parseAddress(bluetooth.deviceAddress).reversedArray() // spec: LSB first

        val nameEir = bluetooth.deviceName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            byteArrayOf((nameBytes.size + 1).toByte(), AD_TYPE_COMPLETE_LOCAL_NAME.toByte()) + nameBytes
        } ?: ByteArray(0)

        val totalLength = 2 + addressBytes.size + nameEir.size // includes the length field itself
        val lengthLe = byteArrayOf((totalLength and 0xFF).toByte(), ((totalLength shr 8) and 0xFF).toByte())
        return lengthLe + addressBytes + nameEir
    }

    /** Accepts "AA:BB:CC:DD:EE:FF" or "AA-BB-CC-DD-EE-FF", case-insensitive. */
    private fun parseAddress(address: String): ByteArray {
        val octets = address.trim().split(":", "-")
        require(octets.size == 6) { "Bluetooth device address must have 6 octets, got \"$address\"" }
        return octets.map {
            it.toIntOrNull(16)?.also { value -> require(value in 0..0xFF) }
                ?: throw IllegalArgumentException("Invalid Bluetooth address octet: \"$it\"")
        }.map { it.toByte() }.toByteArray()
    }
}
