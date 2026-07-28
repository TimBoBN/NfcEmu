package com.nfcemu.ui.share

import com.nfcemu.ndefengine.NdefPayload

/** What kind of content the share-preview screen thinks a piece of shared text is. */
enum class SharedContentType { PHONE, LINK, TEXT }

/**
 * Best-effort classifier for text arriving via the Android share sheet (`ACTION_SEND`,
 * `text/plain`), so [com.nfcemu.ui.share.SharePreviewScreen] can default to the right NDEF
 * type without the user picking one - they can still edit the text to correct a misdetection.
 */
object SharedContentDetector {

    private val phoneRegex = Regex("^[+]?[0-9()\\-\\s./]{5,25}$")
    private val hostRegex = Regex("^[\\w-]+(\\.[\\w-]+)+(/\\S*)?$")

    fun classify(raw: String): SharedContentType {
        val trimmed = raw.trim()
        return when {
            trimmed.matches(phoneRegex) && trimmed.count(Char::isDigit) >= 5 -> SharedContentType.PHONE
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> SharedContentType.LINK
            trimmed.matches(hostRegex) -> SharedContentType.LINK
            else -> SharedContentType.TEXT
        }
    }

    fun toPayload(raw: String, type: SharedContentType): NdefPayload {
        val trimmed = raw.trim()
        return when (type) {
            SharedContentType.PHONE -> NdefPayload.Uri("tel:" + trimmed.filter { it.isDigit() || it == '+' })
            SharedContentType.LINK -> NdefPayload.Uri(
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed",
            )
            SharedContentType.TEXT -> NdefPayload.Text(trimmed)
        }
    }
}
