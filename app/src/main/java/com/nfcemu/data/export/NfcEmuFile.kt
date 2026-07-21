package com.nfcemu.data.export

import com.nfcemu.ndefengine.NdefPayload
import kotlinx.serialization.Serializable

/**
 * On-disk schema of a `.nfcemu` file. Carries both the structured, re-editable
 * profile fields (`profile`) and the raw NDEF bytes actually served over HCE
 * (`ndefBase64`), so the same file works for re-importing into NfcEmu *and* for
 * writing a physical tag with a third-party NFC tool.
 *
 * ```json
 * {
 *   "formatVersion": 1,
 *   "exportedAt": 1732000000000,
 *   "profile": {
 *     "name": "Meine Visitenkarte",
 *     "fields": { "type": "vcard", "name": "Ada Lovelace", ... },
 *     "aarPackageName": null
 *   },
 *   "ndefBase64": "AwoAA1UDaHR0cH..."
 * }
 * ```
 *
 * `fields` is [NdefPayload]'s own polymorphic serialization (discriminator key
 * `"type"`, values `"uri" | "vcard" | "text" | "wifi"`, see that type's kdoc).
 *
 * [formatVersion] is bumped whenever this schema changes in a way older app versions
 * couldn't read; [NfcEmuFileCodec] rejects files with a newer version than it knows.
 */
@Serializable
data class NfcEmuFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportedAt: Long,
    val profile: ExportedProfile,
    val ndefBase64: String,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class ExportedProfile(
    val name: String,
    val fields: NdefPayload,
    val aarPackageName: String? = null,
)
