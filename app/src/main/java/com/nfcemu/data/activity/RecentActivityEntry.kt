package com.nfcemu.data.activity

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One "profile was just activated" event, shown as Home's "Recently sent" list.
 * [typeLabel] is the same coarse discriminator as [com.nfcemu.data.typeLabel] ("uri"/"vcard"/
 * "text"/"wifi"), reused for its icon via [com.nfcemu.ui.components.typeGlyphForLabel].
 */
@Serializable
data class RecentActivityEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val typeLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
)
