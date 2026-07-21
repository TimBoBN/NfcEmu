package com.nfcemu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.graphics.vector.ImageVector
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ui.profileform.ProfileTypeTemplate

/** Maps a payload/template to its icon + a short human label, used across list, quick-select and type picker. */
fun ProfileTypeTemplate.icon(): ImageVector = when (this) {
    ProfileTypeTemplate.WEBSITE -> Icons.Filled.Language
    ProfileTypeTemplate.PHONE -> Icons.Filled.Phone
    ProfileTypeTemplate.EMAIL -> Icons.Filled.Email
    ProfileTypeTemplate.SMS -> Icons.AutoMirrored.Filled.Message
    ProfileTypeTemplate.LOCATION -> Icons.Filled.LocationOn
    ProfileTypeTemplate.PLAY_STORE -> Icons.Filled.Shop
    ProfileTypeTemplate.WIFI -> Icons.Filled.Wifi
    ProfileTypeTemplate.VCARD -> Icons.Filled.ContactPage
    ProfileTypeTemplate.TEXT -> Icons.Filled.TextFields
    ProfileTypeTemplate.CUSTOM_URI -> Icons.Filled.Link
}

fun ProfileTypeTemplate.label(): String = when (this) {
    ProfileTypeTemplate.WEBSITE -> "Website"
    ProfileTypeTemplate.PHONE -> "Telefonnummer"
    ProfileTypeTemplate.EMAIL -> "E-Mail"
    ProfileTypeTemplate.SMS -> "SMS"
    ProfileTypeTemplate.LOCATION -> "Standort"
    ProfileTypeTemplate.PLAY_STORE -> "Play Store App"
    ProfileTypeTemplate.WIFI -> "WLAN-Zugang"
    ProfileTypeTemplate.VCARD -> "Visitenkarte"
    ProfileTypeTemplate.TEXT -> "Text"
    ProfileTypeTemplate.CUSTOM_URI -> "Custom URI"
}

/** Best-effort icon for an already-encoded payload (list/quick-select rows don't know the original template). */
fun NdefPayload.icon(): ImageVector = when (this) {
    is NdefPayload.VCard -> Icons.Filled.ContactPage
    is NdefPayload.Text -> Icons.Filled.TextFields
    is NdefPayload.WifiHandover -> Icons.Filled.Wifi
    is NdefPayload.Uri -> when {
        uri.startsWith("tel:") -> Icons.Filled.Phone
        uri.startsWith("mailto:") -> Icons.Filled.Email
        uri.startsWith("sms:") -> Icons.AutoMirrored.Filled.Message
        uri.startsWith("geo:") -> Icons.Filled.LocationOn
        uri.startsWith("market://") -> Icons.Filled.Shop
        uri.startsWith("http://") || uri.startsWith("https://") -> Icons.Filled.Language
        else -> Icons.Filled.Link
    }
}
