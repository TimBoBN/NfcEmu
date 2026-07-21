package com.nfcemu.ui.components

import com.nfcemu.ndefengine.NdefPayload

/** Short, human-readable summary of what tapping this profile's tag would actually do. */
fun NdefPayload.previewText(): String = when (this) {
    is NdefPayload.Uri -> when {
        uri.startsWith("tel:") -> "Call ${uri.removePrefix("tel:")}"
        uri.startsWith("mailto:") -> "Email ${uri.removePrefix("mailto:").substringBefore('?')}"
        uri.startsWith("sms:") -> "Text ${uri.removePrefix("sms:").substringBefore('?')}"
        uri.startsWith("geo:") -> "Open location ${uri.removePrefix("geo:").substringBefore('?')}"
        uri.startsWith("market://details?id=") -> "Play Store: ${uri.removePrefix("market://details?id=")}"
        uri.startsWith("http://") || uri.startsWith("https://") -> "Open website: $uri"
        else -> "Opens: $uri"
    }
    is NdefPayload.VCard -> "Save contact: ${name ?: "(no name)"}"
    is NdefPayload.Text -> if (text.length > 60) text.take(57) + "…" else text
    is NdefPayload.WifiHandover -> "Connect to Wi-Fi “$ssid”"
}
