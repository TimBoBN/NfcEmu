package com.nfcemu.data

import com.nfcemu.ndefengine.NdefPayload
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A user-defined emulation profile. `fields` reuses [NdefPayload] directly (see that
 * type's kdoc) so the exact same structured data that gets encoded to NDEF bytes is
 * also what's persisted and shown/edited in the UI - no separate mapping layer.
 */
@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fields: NdefPayload,
    val aarPackageName: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    /**
     * Position in Home's quick-select carousel, ascending; `null` means "not shown there".
     * Independent of [pinned] - [pinned] still drives the home screen widget/Quick Settings
     * tile/launcher shortcuts, which weren't part of this manual-curation redesign - see
     * [com.nfcemu.ui.home.HomeViewModel] and [com.nfcemu.data.ProfileRepository.reorderQuickSelect].
     */
    val quickSelectOrder: Int? = null,
) {
    companion object {
        /**
         * Reserved id for the single "My Profile" row: a real [Profile] persisted alongside
         * normal ones (so HCE emulation/Transmit need zero special-casing), but filtered out
         * of every user-facing profile list - see [com.nfcemu.ui.home.HomeViewModel] and
         * [com.nfcemu.ui.profilelist.ProfileListViewModel].
         */
        const val MY_PROFILE_ID = "my-profile"

        /**
         * Reserved id for the ephemeral "shared via Android share sheet" row: a real
         * [Profile] persisted just like [MY_PROFILE_ID] (so HCE emulation/Transmit need zero
         * special-casing), filtered out of every user-facing profile list, and removed again
         * once Transmit closes unless the user explicitly keeps it via "Save as profile" - see
         * [com.nfcemu.data.ProfileRepository.activateShared].
         */
        const val SHARED_ID = "shared"

        /** Ids never shown in the profile list/carousel - see [MY_PROFILE_ID] and [SHARED_ID]. */
        val RESERVED_IDS = setOf(MY_PROFILE_ID, SHARED_ID)
    }
}
