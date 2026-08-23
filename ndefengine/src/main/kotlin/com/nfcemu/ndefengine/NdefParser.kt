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

/** A still-open chunked record sequence: real TNF/type/id come from the first (CF=1) chunk. */
private class PendingChunk(val tnf: Int, val type: ByteArray, val id: ByteArray, val messageBegin: Boolean) {
    val payloadChunks = mutableListOf<ByteArray>()
}

/**
 * Minimal NDEF binary parser. Originally written only to verify, in tests, that
 * [NdefMessageEncoder] output round-trips correctly - now also the parsing step behind
 * [NdefPayloadDecoder], which decodes physically-scanned (and therefore untrusted) third-
 * party tags. Not used by the HCE service at runtime (the service only ever serves
 * pre-built bytes). Throws [IllegalArgumentException] with a descriptive message on
 * truncated/malformed input rather than throwing raw index-out-of-bounds errors.
 *
 * Reassembles chunked records (CF flag, NDEF binary spec section 3.2.6): a record with CF=1
 * carries the real TNF/type/id and is followed by zero or more [Tnf.UNCHANGED] continuation
 * records, the last of which has CF=0. Those are merged into one [ParsedNdefRecord] with the
 * concatenated payload rather than being surfaced as several malformed records.
 */
object NdefParser {

    fun parse(data: ByteArray): List<ParsedNdefRecord> {
        val records = mutableListOf<ParsedNdefRecord>()
        var pending: PendingChunk? = null
        var offset = 0
        while (offset < data.size) {
            val (raw, nextOffset) = parseOneRecord(data, offset)
            offset = nextOffset

            val chunk = pending
            when {
                chunk == null && raw.cf -> pending = PendingChunk(raw.tnf, raw.type, raw.id, raw.mb).apply {
                    payloadChunks.add(raw.payload)
                }
                chunk == null -> records.add(ParsedNdefRecord(raw.tnf, raw.type, raw.id, raw.payload, raw.mb, raw.me))
                else -> {
                    require(raw.tnf == Tnf.UNCHANGED) {
                        "Malformed chunked NDEF record at offset $offset: expected TNF_UNCHANGED continuation"
                    }
                    chunk.payloadChunks.add(raw.payload)
                    if (!raw.cf) {
                        val fullPayload = chunk.payloadChunks.reduce { acc, part -> acc + part }
                        records.add(ParsedNdefRecord(chunk.tnf, chunk.type, chunk.id, fullPayload, chunk.messageBegin, raw.me))
                        pending = null
                    }
                }
            }
        }
        require(pending == null) { "Truncated NDEF message: chunked record was never terminated" }
        return records
    }

    private class RawRecord(
        val tnf: Int,
        val type: ByteArray,
        val id: ByteArray,
        val payload: ByteArray,
        val mb: Boolean,
        val me: Boolean,
        val cf: Boolean,
    )

    private fun parseOneRecord(data: ByteArray, start: Int): Pair<RawRecord, Int> {
        var offset = start
        require(offset < data.size) { "Truncated NDEF message at offset $offset: expected record header" }
        val header = data[offset].toInt() and 0xFF
        val mb = header and 0x80 != 0
        val me = header and 0x40 != 0
        val cf = header and 0x20 != 0
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

        return RawRecord(tnf, type, id, payload, mb, me, cf) to offset
    }
}
