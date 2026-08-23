package com.nfcemu.ui.profileform

import com.nfcemu.ndefengine.WifiAuthType

/**
 * The type tiles shown in the "new profile" flow. Several templates (Website, Phone,
 * Email, Sms, Location, PlayStore, CustomUri) all ultimately produce a
 * [com.nfcemu.ndefengine.NdefPayload.Uri] - they exist as separate templates purely
 * because each needs a different input mask and validation, not because the engine
 * treats them differently.
 */
enum class ProfileTypeTemplate {
    WEBSITE,
    PHONE,
    EMAIL,
    SMS,
    LOCATION,
    PLAY_STORE,
    WIFI,
    BLUETOOTH,
    VCARD,
    TEXT,
    CUSTOM_URI,
}

/** Editable form state, one variant per [ProfileTypeTemplate]. All-String/primitive fields for direct text-field binding. */
sealed interface ProfileFormFields {
    val template: ProfileTypeTemplate

    data class Website(val url: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.WEBSITE
    }

    data class Phone(val number: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.PHONE
    }

    data class Email(val address: String = "", val subject: String = "", val body: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.EMAIL
    }

    data class Sms(val number: String = "", val body: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.SMS
    }

    data class Location(val latitude: String = "", val longitude: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.LOCATION
    }

    data class PlayStore(val appId: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.PLAY_STORE
    }

    data class Wifi(
        val ssid: String = "",
        val authType: WifiAuthType = WifiAuthType.WPA2_PSK,
        val password: String = "",
    ) : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.WIFI
    }

    data class Bluetooth(
        val deviceAddress: String = "",
        val deviceName: String = "",
    ) : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.BLUETOOTH
    }

    data class VCard(
        val name: String = "",
        val phones: List<String> = listOf(""),
        val emails: List<String> = listOf(""),
        val organization: String = "",
        val title: String = "",
        val website: String = "",
        val address: String = "",
    ) : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.VCARD
    }

    data class Text(val text: String = "", val languageCode: String = "de") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.TEXT
    }

    data class CustomUri(val uri: String = "") : ProfileFormFields {
        override val template get() = ProfileTypeTemplate.CUSTOM_URI
    }

    companion object {
        fun initialFor(template: ProfileTypeTemplate): ProfileFormFields = when (template) {
            ProfileTypeTemplate.WEBSITE -> Website()
            ProfileTypeTemplate.PHONE -> Phone()
            ProfileTypeTemplate.EMAIL -> Email()
            ProfileTypeTemplate.SMS -> Sms()
            ProfileTypeTemplate.LOCATION -> Location()
            ProfileTypeTemplate.PLAY_STORE -> PlayStore()
            ProfileTypeTemplate.WIFI -> Wifi()
            ProfileTypeTemplate.BLUETOOTH -> Bluetooth()
            ProfileTypeTemplate.VCARD -> VCard()
            ProfileTypeTemplate.TEXT -> Text()
            ProfileTypeTemplate.CUSTOM_URI -> CustomUri()
        }
    }
}
