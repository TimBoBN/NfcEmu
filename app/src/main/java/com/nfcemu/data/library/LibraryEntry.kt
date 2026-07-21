package com.nfcemu.data.library

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LibraryEntryDirection { EXPORTED, IMPORTED }

/**
 * One row in the "Gespeicherte Dateien" library screen: a `.nfcemu` file the user
 * exported or imported via SAF, plus enough metadata to show it without re-opening
 * the file. [uri] is a persisted (`takePersistableUriPermission`) SAF URI string.
 *
 * [linkedProfileId] tracks the profile created the first time this entry was loaded
 * as active, so re-loading it updates that same profile instead of creating a
 * duplicate every time.
 */
@Serializable
data class LibraryEntry(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val displayName: String,
    val profileName: String,
    val payloadTypeLabel: String,
    val direction: LibraryEntryDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val linkedProfileId: String? = null,
)
