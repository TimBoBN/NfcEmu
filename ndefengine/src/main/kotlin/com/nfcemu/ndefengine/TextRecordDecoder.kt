package com.nfcemu.ndefengine

/** Inverse of [TextRecordEncoder]. */
object TextRecordDecoder {

    data class Decoded(val text: String, val languageCode: String)

    fun decode(payload: ByteArray): Decoded {
        require(payload.isNotEmpty()) { "Text record payload must contain at least the status byte" }
        val statusByte = payload[0].toInt() and 0xFF
        val isUtf16 = statusByte and 0x80 != 0
        val langLength = statusByte and 0x3F
        require(payload.size >= 1 + langLength) { "Text record payload truncated before end of language code" }
        val languageCode = String(payload, 1, langLength, Charsets.US_ASCII)
        val textBytes = payload.copyOfRange(1 + langLength, payload.size)
        val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        return Decoded(text = String(textBytes, charset), languageCode = languageCode)
    }
}
