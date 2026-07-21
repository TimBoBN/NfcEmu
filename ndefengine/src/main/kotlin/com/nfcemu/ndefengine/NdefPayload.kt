package com.nfcemu.ndefengine

/**
 * Domain model for everything NfcEmu can emulate. Adding a new emulated type means:
 * 1. Add a variant here.
 * 2. Write an encoder object producing [RawNdefRecord] (or a list of them).
 * 3. Wire the new branch into [NdefMessageFactory.encodePayloadRecords].
 * See the module README for the full walkthrough.
 */
sealed interface NdefPayload {

    /** Generic URI record (NFC Forum Well Known Type "U"). Covers https/tel/mailto/sms/geo/market/custom. */
    data class Uri(val uri: String) : NdefPayload

    /** vCard 3.0 contact card, delivered as a MIME media record ("text/vcard"). All fields optional. */
    data class VCard(
        val name: String? = null,
        val phones: List<String> = emptyList(),
        val emails: List<String> = emptyList(),
        val organization: String? = null,
        val title: String? = null,
        val website: String? = null,
        val address: String? = null,
    ) : NdefPayload

    /** Plain text record (NFC Forum Well Known Type "T"). */
    data class Text(
        val text: String,
        val languageCode: String = "de",
    ) : NdefPayload

    /** Wi-Fi credentials delivered via NFC Forum Connection Handover + WSC carrier record. */
    data class WifiHandover(
        val ssid: String,
        val authType: WifiAuthType,
        val password: String? = null,
    ) : NdefPayload
}

enum class WifiAuthType {
    OPEN,
    WEP,
    WPA_PSK,
    WPA2_PSK,
}

/** Optional Android Application Record, appended after the primary record to bind a profile to an app. */
data class AarConfig(val packageName: String)
