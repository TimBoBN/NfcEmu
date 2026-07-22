package com.nfcemu.ndefengine

/** Result of decoding an arbitrary, possibly third-party, tag's raw NDEF bytes. */
sealed interface DecodedTagResult {
    data class Success(val payload: NdefPayload, val aarPackageName: String?) : DecodedTagResult
    data class Unsupported(val reason: String) : DecodedTagResult
    data object Empty : DecodedTagResult
}

/**
 * Single entry point for turning a physically-scanned tag's raw NDEF bytes back into a
 * [NdefPayload] - the inverse of [NdefMessageFactory]. Never throws: any parse failure or
 * unrecognized record shape becomes [DecodedTagResult.Unsupported] instead, since a
 * physical tag is untrusted input that won't necessarily follow this app's own encoders'
 * conventions.
 */
object NdefPayloadDecoder {

    private const val AAR_TYPE = "android.com:pkg"

    fun decode(rawNdefBytes: ByteArray): DecodedTagResult {
        if (rawNdefBytes.isEmpty()) return DecodedTagResult.Empty

        val records = try {
            NdefParser.parse(rawNdefBytes)
        } catch (e: IllegalArgumentException) {
            return DecodedTagResult.Unsupported(e.message ?: "Malformed NDEF message")
        }
        if (records.isEmpty()) return DecodedTagResult.Empty

        val aarPackageName = records.lastOrNull(::isAar)?.let { String(it.payload, Charsets.UTF_8) }
        val contentRecords = records.filterNot(::isAar)
        if (contentRecords.isEmpty()) {
            return DecodedTagResult.Unsupported("Tag only contains an Android Application Record, no readable content")
        }

        return try {
            decodeContent(contentRecords, aarPackageName)
        } catch (e: Exception) {
            DecodedTagResult.Unsupported(e.message ?: "Could not decode this tag's content")
        }
    }

    private fun isAar(record: ParsedNdefRecord): Boolean =
        record.tnf == Tnf.EXTERNAL_TYPE && String(record.type, Charsets.US_ASCII) == AAR_TYPE

    private fun decodeContent(records: List<ParsedNdefRecord>, aarPackageName: String?): DecodedTagResult {
        val first = records.first()
        val typeString = String(first.type, Charsets.US_ASCII)

        return when {
            first.tnf == Tnf.WELL_KNOWN && typeString == "U" ->
                DecodedTagResult.Success(NdefPayload.Uri(UriRecordDecoder.decode(first.payload)), aarPackageName)

            first.tnf == Tnf.WELL_KNOWN && typeString == "T" -> {
                val decoded = TextRecordDecoder.decode(first.payload)
                DecodedTagResult.Success(NdefPayload.Text(decoded.text, decoded.languageCode), aarPackageName)
            }

            first.tnf == Tnf.MIME_MEDIA && typeString == "text/vcard" ->
                DecodedTagResult.Success(VCardRecordDecoder.decode(first.payload), aarPackageName)

            first.tnf == Tnf.WELL_KNOWN && typeString == "Hs" -> decodeWifiHandover(records, aarPackageName)

            else -> DecodedTagResult.Unsupported("Unrecognized record type: TNF=${first.tnf}, type=\"$typeString\"")
        }
    }

    private fun decodeWifiHandover(records: List<ParsedNdefRecord>, aarPackageName: String?): DecodedTagResult {
        val carrier = records.firstOrNull {
            it.tnf == Tnf.MIME_MEDIA && String(it.type, Charsets.US_ASCII) == "application/vnd.wfa.wsc"
        } ?: return DecodedTagResult.Unsupported("Handover message did not contain a Wi-Fi Simple Config carrier")

        val wifi = WifiHandoverRecordDecoder.decode(carrier.payload)
            ?: return DecodedTagResult.Unsupported("Could not parse Wi-Fi credential data")
        return DecodedTagResult.Success(wifi, aarPackageName)
    }
}
