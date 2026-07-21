package com.nfcemu.data.export

import com.nfcemu.data.Profile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes [NfcEmuFile]s. Decoding is defensive by design: any malformed,
 * truncated, or incompatible input is turned into a [NfcEmuFileException] rather than
 * an uncaught runtime exception, since the input always comes from a file the user
 * (or another app entirely) picked via SAF - never trust it. Uses Kotlin's own stdlib
 * Base64 (not `android.util.Base64`) so this whole codec is a plain JVM unit, testable
 * without Robolectric/instrumentation.
 */
@OptIn(ExperimentalEncodingApi::class)
object NfcEmuFileCodec {

    private val json = Json {
        ignoreUnknownKeys = true // forward-compatible: tolerate fields added by a future format version
        prettyPrint = true
        isLenient = false
    }

    fun encode(profile: Profile, ndefBytes: ByteArray): String {
        val file = NfcEmuFile(
            exportedAt = System.currentTimeMillis(),
            profile = ExportedProfile(
                name = profile.name,
                fields = profile.fields,
                aarPackageName = profile.aarPackageName,
            ),
            ndefBase64 = Base64.Default.encode(ndefBytes),
        )
        return json.encodeToString(file)
    }

    /** @throws NfcEmuFileException if [content] is not a valid, supported `.nfcemu` file. */
    fun decode(content: String): DecodedNfcEmuFile {
        val file = try {
            json.decodeFromString<NfcEmuFile>(content)
        } catch (e: SerializationException) {
            throw NfcEmuFileException.Corrupt(e.message ?: "JSON konnte nicht gelesen werden")
        } catch (e: IllegalArgumentException) {
            throw NfcEmuFileException.Corrupt(e.message ?: "Ungültige Feldwerte")
        }

        if (file.formatVersion > NfcEmuFile.CURRENT_FORMAT_VERSION) {
            throw NfcEmuFileException.UnsupportedVersion(file.formatVersion, NfcEmuFile.CURRENT_FORMAT_VERSION)
        }
        if (file.profile.name.isBlank()) {
            throw NfcEmuFileException.Corrupt("Profilname fehlt")
        }

        val ndefBytes = try {
            Base64.Default.decode(file.ndefBase64)
        } catch (e: IllegalArgumentException) {
            throw NfcEmuFileException.Corrupt("NDEF-Daten sind nicht gültig Base64-kodiert")
        }

        val profile = Profile(
            name = file.profile.name,
            fields = file.profile.fields,
            aarPackageName = file.profile.aarPackageName,
        )
        return DecodedNfcEmuFile(profile = profile, rawNdefBytes = ndefBytes, exportedAt = file.exportedAt)
    }
}

data class DecodedNfcEmuFile(
    val profile: Profile,
    val rawNdefBytes: ByteArray,
    val exportedAt: Long,
)
