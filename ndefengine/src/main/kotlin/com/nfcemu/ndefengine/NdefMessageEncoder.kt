package com.nfcemu.ndefengine

/**
 * Serializes a list of [RawNdefRecord] into a binary NDEF message per the NFC Forum
 * NDEF binary encoding (NDEF Technical Specification 1.0, section 2).
 *
 * MB (Message Begin) / ME (Message End) are derived from the record's position in
 * the list. Chunked records (CF=1) are never produced - all our payloads are encoded
 * as complete records, so every record is its own first-and-only chunk.
 */
object NdefMessageEncoder {

    fun encode(records: List<RawNdefRecord>): ByteArray {
        require(records.isNotEmpty()) { "NDEF message must contain at least one record" }
        val out = ArrayList<Byte>(256)
        records.forEachIndexed { index, record ->
            require(record.type.size <= 0xFF) { "Type field too long (max 255 bytes)" }
            require(record.id.size <= 0xFF) { "Id field too long (max 255 bytes)" }
            require(record.payload.size >= 0) { "Payload length invalid" }

            val mb = index == 0
            val me = index == records.size - 1
            val sr = record.payload.size <= 0xFF
            val il = record.id.isNotEmpty()

            var header = 0
            if (mb) header = header or 0x80
            if (me) header = header or 0x40
            // CF (chunk flag) intentionally always 0
            if (sr) header = header or 0x10
            if (il) header = header or 0x08
            header = header or (record.tnf and 0x07)

            out.add(header.toByte())
            out.add(record.type.size.toByte())

            if (sr) {
                out.add(record.payload.size.toByte())
            } else {
                val len = record.payload.size.toLong() and 0xFFFFFFFFL
                out.add(((len shr 24) and 0xFF).toByte())
                out.add(((len shr 16) and 0xFF).toByte())
                out.add(((len shr 8) and 0xFF).toByte())
                out.add((len and 0xFF).toByte())
            }

            if (il) {
                out.add(record.id.size.toByte())
            }

            record.type.forEach { out.add(it) }
            if (il) record.id.forEach { out.add(it) }
            record.payload.forEach { out.add(it) }
        }
        return out.toByteArray()
    }
}
