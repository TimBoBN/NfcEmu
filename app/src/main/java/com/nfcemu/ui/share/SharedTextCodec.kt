package com.nfcemu.ui.share

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Round-trips raw shared text through a nav-argument-safe string - same approach as
 * [com.nfcemu.ui.scantag.ScannedPayloadCodec], just without the JSON layer since the payload
 * here is already a plain string.
 */
@OptIn(ExperimentalEncodingApi::class)
object SharedTextCodec {

    fun encode(text: String): String = Base64.UrlSafe.encode(text.toByteArray(Charsets.UTF_8))

    fun decode(encoded: String): String? = runCatching {
        String(Base64.UrlSafe.decode(encoded), Charsets.UTF_8)
    }.getOrNull()
}
