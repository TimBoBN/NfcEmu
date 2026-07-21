package com.nfcemu.ndefengine

/**
 * NFC Forum URI Record Type Definition (RTD-URI 1.0). The first payload byte is a
 * prefix-abbreviation code (0x00-0x23); if the URI starts with one of the standard
 * prefixes it is stripped and replaced by the matching code, otherwise 0x00 ("no
 * abbreviation") is used and the full URI is stored verbatim.
 */
object UriRecordEncoder {

    // Index == abbreviation code. Index 0 ("") means "store as-is".
    private val PREFIXES = listOf(
        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
        "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:",
        "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://",
        "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
    )

    fun encode(uri: String): RawNdefRecord {
        var bestCode = 0
        var bestLen = 0
        for (code in 1 until PREFIXES.size) {
            val prefix = PREFIXES[code]
            if (uri.startsWith(prefix) && prefix.length > bestLen) {
                bestCode = code
                bestLen = prefix.length
            }
        }
        val remainder = uri.substring(bestLen)
        val payload = byteArrayOf(bestCode.toByte()) + remainder.toByteArray(Charsets.UTF_8)
        return RawNdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = byteArrayOf('U'.code.toByte()),
            payload = payload,
        )
    }
}
