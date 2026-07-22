package com.nfcemu.ui.scantag

import com.nfcemu.ndefengine.NdefPayload
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ScannedTag(val payload: NdefPayload, val aarPackageName: String? = null)

/**
 * Round-trips a scanned [ScannedTag] through a nav-argument-safe string: JSON via
 * kotlinx.serialization (the same polymorphic format [com.nfcemu.data.export.NfcEmuFileCodec]
 * uses for `.nfcemu` files), then URL-safe Base64 so the result is a single opaque
 * Navigation-Compose path segment regardless of what characters the JSON contains. Passing
 * the payload this way (rather than an in-memory holder) survives process death.
 */
@OptIn(ExperimentalEncodingApi::class)
object ScannedPayloadCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(tag: ScannedTag): String =
        Base64.UrlSafe.encode(json.encodeToString(tag).toByteArray(Charsets.UTF_8))

    fun decode(encoded: String): ScannedTag? = runCatching {
        json.decodeFromString<ScannedTag>(String(Base64.UrlSafe.decode(encoded), Charsets.UTF_8))
    }.getOrNull()
}
