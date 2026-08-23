package com.nfcemu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.nfcemu.R
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ui.profileform.ProfileTypeTemplate

/**
 * Every profile type shows an icon except Text, which the design renders as a stylized "Aa"
 * text glyph instead - see [TypeIconBadge], the sole consumer of this type.
 */
sealed interface TypeGlyph {
    data class Icon(val imageVector: ImageVector) : TypeGlyph
    data object Text : TypeGlyph
}

/** Maps a payload/template to its glyph + a short human label, used across list, quick-select and type picker. */
@Composable
fun ProfileTypeTemplate.typeGlyph(): TypeGlyph = when (this) {
    ProfileTypeTemplate.WEBSITE -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_website))
    ProfileTypeTemplate.PHONE -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_phone))
    ProfileTypeTemplate.EMAIL -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_email))
    ProfileTypeTemplate.SMS -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_sms))
    ProfileTypeTemplate.LOCATION -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_location))
    ProfileTypeTemplate.PLAY_STORE -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_play_store))
    ProfileTypeTemplate.WIFI -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_wifi))
    ProfileTypeTemplate.BLUETOOTH -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_bluetooth))
    ProfileTypeTemplate.VCARD -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_vcard))
    ProfileTypeTemplate.TEXT -> TypeGlyph.Text
    ProfileTypeTemplate.CUSTOM_URI -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_custom_uri))
}

fun ProfileTypeTemplate.label(): String = when (this) {
    ProfileTypeTemplate.WEBSITE -> "Website"
    ProfileTypeTemplate.PHONE -> "Phone number"
    ProfileTypeTemplate.EMAIL -> "Email"
    ProfileTypeTemplate.SMS -> "SMS"
    ProfileTypeTemplate.LOCATION -> "Location"
    ProfileTypeTemplate.PLAY_STORE -> "Play Store app"
    ProfileTypeTemplate.WIFI -> "Wi-Fi access"
    ProfileTypeTemplate.BLUETOOTH -> "Bluetooth pairing"
    ProfileTypeTemplate.VCARD -> "Business card"
    ProfileTypeTemplate.TEXT -> "Text"
    ProfileTypeTemplate.CUSTOM_URI -> "Custom URI"
}

/** Best-effort glyph for an already-encoded payload (list/quick-select rows don't know the original template). */
@Composable
fun NdefPayload.typeGlyph(): TypeGlyph = when (this) {
    is NdefPayload.VCard -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_vcard))
    is NdefPayload.Text -> TypeGlyph.Text
    is NdefPayload.WifiHandover -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_wifi))
    is NdefPayload.BluetoothHandover -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_bluetooth))
    is NdefPayload.Uri -> when {
        uri.startsWith("tel:") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_phone))
        uri.startsWith("mailto:") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_email))
        uri.startsWith("sms:") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_sms))
        uri.startsWith("geo:") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_location))
        uri.startsWith("market://") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_play_store))
        uri.startsWith("http://") || uri.startsWith("https://") -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_website))
        else -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_custom_uri))
    }
}

/** Human label for an already-encoded payload - same scheme-sniffing as [NdefPayload.typeGlyph], used by Transmit's type-label line. */
fun NdefPayload.typeDisplayLabel(): String = when (this) {
    is NdefPayload.VCard -> "Business card"
    is NdefPayload.Text -> "Text"
    is NdefPayload.WifiHandover -> "Wi-Fi access"
    is NdefPayload.BluetoothHandover -> "Bluetooth pairing"
    is NdefPayload.Uri -> when {
        uri.startsWith("tel:") -> "Phone number"
        uri.startsWith("mailto:") -> "Email"
        uri.startsWith("sms:") -> "SMS"
        uri.startsWith("geo:") -> "Location"
        uri.startsWith("market://") -> "Play Store app"
        uri.startsWith("http://") || uri.startsWith("https://") -> "Website"
        else -> "Custom URI"
    }
}

/**
 * Glyph for a [com.nfcemu.data.library.LibraryEntry], which only stores the coarse
 * [com.nfcemu.data.typeLabel] discriminator ("uri"/"vcard"/"text"/"wifi"), not the full
 * payload - so, unlike [NdefPayload.typeGlyph], this can't distinguish a website from a
 * phone number, both being "uri". The single mapping this and [NdefPayload.typeGlyph] both
 * ultimately draw from is the same vector drawable set, just keyed differently out of
 * necessity.
 */
@Composable
fun typeGlyphForLabel(label: String): TypeGlyph = when (label) {
    "vcard" -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_vcard))
    "text" -> TypeGlyph.Text
    "wifi" -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_wifi))
    "bluetooth" -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_bluetooth))
    else -> TypeGlyph.Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_website))
}
