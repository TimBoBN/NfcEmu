package com.nfcemu.data.contacts

import kotlinx.serialization.Serializable
import java.util.UUID

/** A contact card received from another device via NFC (see `ui/receivecontact`). */
@Serializable
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val organization: String? = null,
    val receivedAt: Long = System.currentTimeMillis(),
)
