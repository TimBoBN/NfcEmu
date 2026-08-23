package com.nfcemu.ndefengine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain model for everything NfcEmu can emulate. `@Serializable` so this same model
 * doubles as the persisted, re-editable profile field data in the `.nfcemu` file
 * format (see the app module's `data.export` package) - no separate mapper needed.
 *
 * Adding a new emulated type means:
 * 1. Add a variant here (with a stable `@SerialName` - it ends up in exported files).
 * 2. Write an encoder object producing [RawNdefRecord] (or a list of them).
 * 3. Wire the new branch into [NdefMessageFactory.encodePayloadRecords].
 * See the module README for the full walkthrough.
 */
@Serializable
sealed interface NdefPayload {

    /** Generic URI record (NFC Forum Well Known Type "U"). Covers https/tel/mailto/sms/geo/market/custom. */
    @Serializable
    @SerialName("uri")
    data class Uri(val uri: String) : NdefPayload

    /** vCard 3.0 contact card, delivered as a MIME media record ("text/vcard"). All fields optional. */
    @Serializable
    @SerialName("vcard")
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
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
        val languageCode: String = "de",
    ) : NdefPayload

    /** Wi-Fi credentials delivered via NFC Forum Connection Handover + WSC carrier record. */
    @Serializable
    @SerialName("wifi")
    data class WifiHandover(
        val ssid: String,
        val authType: WifiAuthType,
        val password: String? = null,
    ) : NdefPayload

    /** Bluetooth pairing info delivered via NFC Forum Connection Handover + Bluetooth OOB carrier record. */
    @Serializable
    @SerialName("bluetooth")
    data class BluetoothHandover(
        val deviceAddress: String,
        val deviceName: String? = null,
    ) : NdefPayload
}

@Serializable
enum class WifiAuthType {
    OPEN,
    WEP,
    WPA_PSK,
    WPA2_PSK,
}

/** Optional Android Application Record, appended after the primary record to bind a profile to an app. */
@Serializable
data class AarConfig(val packageName: String)
