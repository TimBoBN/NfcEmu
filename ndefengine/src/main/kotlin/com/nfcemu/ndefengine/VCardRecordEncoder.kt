package com.nfcemu.ndefengine

/**
 * Encodes a [NdefPayload.VCard] as a vCard 3.0 text body, delivered as a MIME media
 * NDEF record (type "text/vcard"). All fields are optional; empty/blank values are
 * omitted entirely rather than emitted as empty properties.
 */
object VCardRecordEncoder {

    private const val MIME_TYPE = "text/vcard"

    fun encode(vcard: NdefPayload.VCard): RawNdefRecord {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\r\n")
        sb.append("VERSION:3.0\r\n")

        vcard.name?.takeIf { it.isNotBlank() }?.let { name ->
            sb.append("FN:").append(escape(name)).append("\r\n")
            sb.append("N:").append(escape(name)).append(";;;;\r\n")
        }
        vcard.phones.filter { it.isNotBlank() }.forEach { phone ->
            sb.append("TEL;TYPE=CELL:").append(escape(phone)).append("\r\n")
        }
        vcard.emails.filter { it.isNotBlank() }.forEach { email ->
            sb.append("EMAIL:").append(escape(email)).append("\r\n")
        }
        vcard.organization?.takeIf { it.isNotBlank() }?.let {
            sb.append("ORG:").append(escape(it)).append("\r\n")
        }
        vcard.title?.takeIf { it.isNotBlank() }?.let {
            sb.append("TITLE:").append(escape(it)).append("\r\n")
        }
        vcard.website?.takeIf { it.isNotBlank() }?.let {
            sb.append("URL:").append(escape(it)).append("\r\n")
        }
        vcard.address?.takeIf { it.isNotBlank() }?.let {
            // ADR components: PO Box;Extended;Street;City;Region;Postal;Country
            // We only have one free-text field, so it goes into "Street".
            sb.append("ADR:;;").append(escape(it)).append(";;;;\r\n")
        }
        sb.append("END:VCARD\r\n")

        return RawNdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = MIME_TYPE.toByteArray(Charsets.US_ASCII),
            payload = sb.toString().toByteArray(Charsets.UTF_8),
        )
    }

    /** Escapes vCard 3.0 special characters per RFC 2426 section 5.8.4. */
    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
}
