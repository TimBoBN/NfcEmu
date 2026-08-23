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

    /**
     * A tag can carry several records (e.g. a URI record followed by a Text record); only one
     * becomes this app's [NdefPayload], so this picks the first record whose shape is actually
     * decodable rather than always looking at `records.first()` - a recognized record buried
     * behind e.g. a foreign/unsupported leading record would otherwise be silently missed.
     * Handover Select ("Hs") is looked up across the whole list on top of that, since its
     * carrier record lives alongside it rather than being the thing itself.
     */
    private fun decodeContent(records: List<ParsedNdefRecord>, aarPackageName: String?): DecodedTagResult {
        if (records.any { it.tnf == Tnf.WELL_KNOWN && typeStringOf(it) == "Hs" }) {
            return decodeConnectionHandover(records, aarPackageName)
        }

        val recognized = records.firstOrNull(::isRecognized)
            ?: return DecodedTagResult.Unsupported(
                "Unrecognized record type: TNF=${records.first().tnf}, type=\"${typeStringOf(records.first())}\"",
            )
        return decodeRecord(recognized, aarPackageName)
    }

    private fun typeStringOf(record: ParsedNdefRecord): String = String(record.type, Charsets.US_ASCII)

    private fun isRecognized(record: ParsedNdefRecord): Boolean {
        val typeString = typeStringOf(record)
        return (record.tnf == Tnf.WELL_KNOWN && (typeString == "U" || typeString == "T" || typeString == "Sp")) ||
            (record.tnf == Tnf.MIME_MEDIA && (typeString == "text/vcard" || typeString == "text/x-vcard"))
    }

    private fun decodeRecord(record: ParsedNdefRecord, aarPackageName: String?): DecodedTagResult {
        val typeString = typeStringOf(record)

        return when {
            record.tnf == Tnf.WELL_KNOWN && typeString == "U" ->
                DecodedTagResult.Success(NdefPayload.Uri(UriRecordDecoder.decode(record.payload)), aarPackageName)

            record.tnf == Tnf.WELL_KNOWN && typeString == "T" -> {
                val decoded = TextRecordDecoder.decode(record.payload)
                DecodedTagResult.Success(NdefPayload.Text(decoded.text, decoded.languageCode), aarPackageName)
            }

            record.tnf == Tnf.WELL_KNOWN && typeString == "Sp" -> decodeSmartPoster(record, aarPackageName)

            record.tnf == Tnf.MIME_MEDIA && (typeString == "text/vcard" || typeString == "text/x-vcard") ->
                DecodedTagResult.Success(VCardRecordDecoder.decode(record.payload), aarPackageName)

            else -> error("unreachable: $record was already filtered by isRecognized")
        }
    }

    /**
     * Smart Poster (NFC Forum RTD-SP): its payload is itself a nested NDEF message, normally a
     * mandatory "U" record plus optional decoration (title, icon, action) this app has no field
     * for. Only the wrapped URI is surfaced - that's the part a reader actually acts on.
     */
    private fun decodeSmartPoster(record: ParsedNdefRecord, aarPackageName: String?): DecodedTagResult {
        val inner = try {
            NdefParser.parse(record.payload)
        } catch (e: IllegalArgumentException) {
            return DecodedTagResult.Unsupported(e.message ?: "Malformed Smart Poster payload")
        }
        val uriRecord = inner.firstOrNull { it.tnf == Tnf.WELL_KNOWN && typeStringOf(it) == "U" }
            ?: return DecodedTagResult.Unsupported("Smart Poster did not contain a URI record")
        return DecodedTagResult.Success(NdefPayload.Uri(UriRecordDecoder.decode(uriRecord.payload)), aarPackageName)
    }

    /**
     * A Handover Select message can in principle carry several alternative carriers; this app
     * only ever emits one, so it picks the first carrier type it recognizes rather than trying
     * to represent "pick one of several" in [NdefPayload].
     */
    private fun decodeConnectionHandover(records: List<ParsedNdefRecord>, aarPackageName: String?): DecodedTagResult {
        val wifiCarrier = records.firstOrNull { it.tnf == Tnf.MIME_MEDIA && typeStringOf(it) == "application/vnd.wfa.wsc" }
        if (wifiCarrier != null) {
            val wifi = WifiHandoverRecordDecoder.decode(wifiCarrier.payload)
                ?: return DecodedTagResult.Unsupported("Could not parse Wi-Fi credential data")
            return DecodedTagResult.Success(wifi, aarPackageName)
        }

        val bluetoothCarrier = records.firstOrNull { it.tnf == Tnf.MIME_MEDIA && typeStringOf(it) == "application/vnd.bluetooth.ep.oob" }
        if (bluetoothCarrier != null) {
            val bluetooth = BluetoothHandoverRecordDecoder.decode(bluetoothCarrier.payload)
                ?: return DecodedTagResult.Unsupported("Could not parse Bluetooth pairing data")
            return DecodedTagResult.Success(bluetooth, aarPackageName)
        }

        return DecodedTagResult.Unsupported("Handover message did not contain a Wi-Fi or Bluetooth carrier")
    }
}
