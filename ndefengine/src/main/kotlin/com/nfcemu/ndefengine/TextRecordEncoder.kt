package com.nfcemu.ndefengine

/**
 * NFC Forum Text Record Type Definition (RTD-Text 1.0). Payload = status byte
 * (bit 7 = 0 for UTF-8, bits 5-0 = language code length) + ISO/IANA language code
 * (ASCII) + UTF-8 text.
 */
object TextRecordEncoder {

    fun encode(text: String, languageCode: String = "de"): RawNdefRecord {
        val langBytes = languageCode.toByteArray(Charsets.US_ASCII)
        require(langBytes.size <= 0x3F) { "Language code must be at most 63 bytes" }
        val statusByte = (langBytes.size and 0x3F).toByte() // bit 7 = 0 => UTF-8
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = byteArrayOf(statusByte) + langBytes + textBytes
        return RawNdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = byteArrayOf('T'.code.toByte()),
            payload = payload,
        )
    }
}
