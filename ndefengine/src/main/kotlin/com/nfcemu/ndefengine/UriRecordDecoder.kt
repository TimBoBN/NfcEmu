package com.nfcemu.ndefengine

/** Inverse of [UriRecordEncoder]: reconstitutes the original URI from an RTD-URI payload. */
object UriRecordDecoder {

    fun decode(payload: ByteArray): String {
        require(payload.isNotEmpty()) { "URI record payload must contain at least the prefix code byte" }
        val code = payload[0].toInt() and 0xFF
        val prefix = UriRecordEncoder.PREFIXES.getOrNull(code)
            ?: throw IllegalArgumentException("Unknown URI abbreviation code: $code")
        val remainder = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return prefix + remainder
    }
}
