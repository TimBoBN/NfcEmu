package com.nfcemu.ui.components

import com.nfcemu.ndefengine.NdefPayload

/** Short, human-readable summary of what tapping this profile's tag would actually do. */
fun NdefPayload.previewText(): String = when (this) {
    is NdefPayload.Uri -> when {
        uri.startsWith("tel:") -> "Anruf bei ${uri.removePrefix("tel:")}"
        uri.startsWith("mailto:") -> "E-Mail an ${uri.removePrefix("mailto:").substringBefore('?')}"
        uri.startsWith("sms:") -> "SMS an ${uri.removePrefix("sms:").substringBefore('?')}"
        uri.startsWith("geo:") -> "Standort ${uri.removePrefix("geo:").substringBefore('?')} öffnen"
        uri.startsWith("market://details?id=") -> "Play Store: ${uri.removePrefix("market://details?id=")}"
        uri.startsWith("http://") || uri.startsWith("https://") -> "Website öffnen: $uri"
        else -> "Öffnet: $uri"
    }
    is NdefPayload.VCard -> "Kontakt speichern: ${name ?: "(ohne Namen)"}"
    is NdefPayload.Text -> if (text.length > 60) text.take(57) + "…" else text
    is NdefPayload.WifiHandover -> "Mit WLAN „$ssid“ verbinden"
}
