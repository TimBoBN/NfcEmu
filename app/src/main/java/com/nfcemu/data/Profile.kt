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
)
