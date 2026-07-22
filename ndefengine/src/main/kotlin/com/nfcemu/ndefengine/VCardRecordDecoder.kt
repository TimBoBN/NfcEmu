package com.nfcemu.ndefengine

/**
 * Inverse of [VCardRecordEncoder]. A real third-party tag won't necessarily follow this
 * app's own encoder's exact conventions, so parsing here is intentionally lenient:
 * unrecognized or malformed property lines are skipped rather than treated as fatal, and
 * folded continuation lines (RFC 2425 - a line starting with a space or tab continues the
 * previous one) are unfolded before parsing.
 */
object VCardRecordDecoder {

    private val PROPERTY_LINE = Regex("^([A-Za-z]+)(;[^:]*)?:(.*)$")

    fun decode(payload: ByteArray): NdefPayload.VCard {
        val lines = unfold(String(payload, Charsets.UTF_8))

        var name: String? = null
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var organization: String? = null
        var title: String? = null
        var website: String? = null
        var address: String? = null

        for (line in lines) {
            val match = PROPERTY_LINE.matchEntire(line) ?: continue
            val (rawProperty, _, rawValue) = match.destructured
            when (rawProperty.uppercase()) {
                "FN" -> if (name == null) name = unescape(rawValue)
                "TEL" -> unescape(rawValue).takeIf { it.isNotBlank() }?.let { phones += it }
                "EMAIL" -> unescape(rawValue).takeIf { it.isNotBlank() }?.let { emails += it }
                "ORG" -> organization = unescape(rawValue).takeIf { it.isNotBlank() }
                "TITLE" -> title = unescape(rawValue).takeIf { it.isNotBlank() }
                "URL" -> website = unescape(rawValue).takeIf { it.isNotBlank() }
                // ADR components: PO Box;Extended;Street;City;Region;Postal;Country - the
                // encoder only ever fills "Street" (index 2). Structural ';' separators must
                // be split *before* unescaping, or an escaped "\;" inside the free text would
                // be indistinguishable from a real component boundary.
                "ADR" -> address = splitUnescaped(rawValue, ';').getOrNull(2)?.let(::unescape)?.takeIf { it.isNotBlank() }
            }
        }

        return NdefPayload.VCard(
            name = name,
            phones = phones,
            emails = emails,
            organization = organization,
            title = title,
            website = website,
            address = address,
        )
    }

    private fun unfold(text: String): List<String> {
        val rawLines = text.split("\r\n", "\n").map { it.trimEnd('\r') }
        val unfolded = mutableListOf<String>()
        for (line in rawLines) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && unfolded.isNotEmpty()) {
                unfolded[unfolded.lastIndex] = unfolded.last() + line.substring(1)
            } else {
                unfolded.add(line)
            }
        }
        return unfolded.filter { it.isNotBlank() }
    }

    /** Splits on [delimiter], treating a backslash-escaped delimiter as a literal character. */
    private fun splitUnescaped(value: String, delimiter: Char): List<String> {
        val parts = mutableListOf(StringBuilder())
        var escaped = false
        for (ch in value) {
            when {
                escaped -> {
                    parts.last().append(ch)
                    escaped = false
                }
                ch == '\\' -> {
                    parts.last().append(ch)
                    escaped = true
                }
                ch == delimiter -> parts.add(StringBuilder())
                else -> parts.last().append(ch)
            }
        }
        return parts.map { it.toString() }
    }

    /** Reverses [VCardRecordEncoder]'s escape, in the exact reverse order it was applied. */
    private fun unescape(value: String): String =
        value
            .replace("\\n", "\n")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
}
