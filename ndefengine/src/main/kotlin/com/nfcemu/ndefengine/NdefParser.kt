package com.nfcemu.ndefengine

/** Decoded record as read back from binary, mirroring the fields of [RawNdefRecord] plus flags. */
data class ParsedNdefRecord(
    val tnf: Int,
    val type: ByteArray,
    val id: ByteArray,
    val payload: ByteArray,
    val messageBegin: Boolean,
    val messageEnd: Boolean,
)

/**
 * Minimal NDEF binary parser used only to verify, in tests, that [NdefMessageEncoder]
 * output round-trips correctly. Not used by the HCE service at runtime (the service
 * only ever serves pre-built bytes). Throws [IllegalArgumentException] with a
 * descriptive message on truncated/malformed input rather than throwing raw
 * index-out-of-bounds errors.
 */
object NdefParser {

    fun parse(data: ByteArray): List<ParsedNdefRecord> {
        val records = mutableListOf<ParsedNdefRecord>()
        var offset = 0
        while (offset < data.size) {
            offset = parseOneRecord(data, offset, records)
        }
        return records
    }

    private fun parseOneRecord(data: ByteArray, start: Int, out: MutableList<ParsedNdefRecord>): Int {
        var offset = start
        require(offset < data.size) { "Truncated NDEF message at offset $offset: expected record header" }
        val header = data[offset].toInt() and 0xFF
        val mb = header and 0x80 != 0
        val me = header and 0x40 != 0
        val sr = header and 0x10 != 0
        val il = header and 0x08 != 0
        val tnf = header and 0x07
        offset += 1

        require(offset < data.size) { "Truncated NDEF message at offset $offset: expected type length" }
        val typeLength = data[offset].toInt() and 0xFF
        offset += 1

        val payloadLength: Int
        if (sr) {
            require(offset < data.size) { "Truncated NDEF message at offset $offset: expected short payload length" }
            payloadLength = data[offset].toInt() and 0xFF
            offset += 1
        } else {
            require(offset + 4 <= data.size) { "Truncated NDEF message at offset $offset: expected payload length" }
            payloadLength = ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
            offset += 4
        }

        val idLength = if (il) {
            require(offset < data.size) { "Truncated NDEF message at offset $offset: expected id length" }
            val length = data[offset].toInt() and 0xFF
            offset += 1
            length
        } else 0

        require(offset + typeLength <= data.size) { "Truncated NDEF message at offset $offset: expected type field" }
        val type = data.copyOfRange(offset, offset + typeLength)
        offset += typeLength

        val id = if (idLength > 0) {
            require(offset + idLength <= data.size) { "Truncated NDEF message at offset $offset: expected id field" }
            val value = data.copyOfRange(offset, offset + idLength)
            offset += idLength
            value
        } else ByteArray(0)

        require(offset + payloadLength <= data.size) { "Truncated NDEF message at offset $offset: expected payload field" }
        val payload = data.copyOfRange(offset, offset + payloadLength)
        offset += payloadLength

        out.add(ParsedNdefRecord(tnf, type, id, payload, mb, me))
        return offset
    }
}
