package com.nfcemu.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nfcemu.data.export.DecodedNfcEmuFile
import com.nfcemu.data.export.LibraryZipCodec
import com.nfcemu.data.export.NfcEmuFileCodec
import com.nfcemu.data.library.LibraryDataStore
import com.nfcemu.data.library.LibraryEntry
import com.nfcemu.data.library.LibraryEntryDirection
import com.nfcemu.ndefengine.AarConfig
import com.nfcemu.ndefengine.NdefMessageFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Result of [FileRepository.importLibraryFromZip]: how many of the ZIP's entries decoded successfully. */
data class BulkImportSummary(val imported: Int, val skipped: Int)

/**
 * Storage Access Framework based export/import of `.nfcemu` files, plus the raw NDEF
 * binary dump variant for use with third-party NFC tools. All file I/O runs on
 * [Dispatchers.IO]; nothing here ever touches external storage without the user
 * having picked a document via [Intent.ACTION_CREATE_DOCUMENT] / [Intent.ACTION_OPEN_DOCUMENT]
 * first (the resulting [Uri] is handed in by the caller after the SAF picker returns).
 */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val libraryDataStore: LibraryDataStore,
) {

    val libraryEntries: Flow<List<LibraryEntry>> = libraryDataStore.entries

    suspend fun exportProfile(profile: Profile, targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ndefBytes = encode(profile)
            val json = NfcEmuFileCodec.encode(profile, ndefBytes)
            writeText(targetUri, json)
            recordLibraryEntry(profile, targetUri, LibraryEntryDirection.EXPORTED)
        }
    }

    suspend fun exportRawNdef(profile: Profile, targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            writeBytes(targetUri, encode(profile))
        }
    }

    /**
     * Exports every given profile as its own unmodified `.nfcemu` file bundled into one ZIP
     * (see [LibraryZipCodec]). Not recorded in the library list: unlike a single-file export,
     * individual profiles inside the ZIP aren't separately re-openable via [targetUri] once
     * this stream closes, so there's no URI a library entry could correctly point back at.
     */
    suspend fun exportLibraryAsZip(profiles: List<Profile>, targetUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val zipBytes = LibraryZipCodec.buildZip(profiles.map { it to encode(it) })
            writeBytes(targetUri, zipBytes)
        }
    }

    /**
     * Imports every `.nfcemu` file found in the ZIP as a new, inactive profile (see
     * [LibraryZipCodec] for how corrupt/foreign entries are skipped rather than aborting the
     * whole import). Imports run sequentially (not launched concurrently): [ProfileRepository]'s
     * mutators read the DataStore's current state fresh on every call specifically to avoid a
     * stale-snapshot race (see its kdoc) - parallel imports here would reintroduce exactly that
     * bug. Unlike [importProfile], these profiles are not recorded in the library list - see
     * [exportLibraryAsZip]'s kdoc for why a ZIP entry has no reload-able URI of its own.
     */
    suspend fun importLibraryFromZip(sourceUri: Uri): Result<BulkImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val zipBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: error("Could not open source file")
            val extracted = LibraryZipCodec.extractProfiles(zipBytes)
            for (profile in extracted.profiles) {
                profileRepository.importProfile(profile)
            }
            BulkImportSummary(imported = extracted.profiles.size, skipped = extracted.skipped)
        }
    }

    /** Imports a `.nfcemu` file as a new, inactive profile. Never overwrites an existing profile. */
    suspend fun importProfile(sourceUri: Uri): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decode(sourceUri)
            val stored = profileRepository.importProfile(decoded.profile)
            takePersistableReadPermission(sourceUri)
            recordLibraryEntry(stored, sourceUri, LibraryEntryDirection.IMPORTED, linkedProfileId = stored.id)
            stored
        }
    }

    /**
     * Re-reads a library entry's file and makes it the active profile. The first time
     * this is called for a given entry it creates the backing profile (and links it on
     * the entry); subsequent calls update that same profile instead of duplicating it.
     */
    suspend fun loadEntryAsActiveProfile(entry: LibraryEntry): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decode(Uri.parse(entry.uri))
            val existingId = entry.linkedProfileId
            val resolvedId = if (existingId != null && profileRepository.profiles.value.any { it.id == existingId }) {
                profileRepository.updateProfile(decoded.profile.copy(id = existingId))
                existingId
            } else {
                val stored = profileRepository.importProfile(decoded.profile)
                updateLinkedProfileId(entry.id, stored.id)
                stored.id
            }
            profileRepository.setActive(resolvedId)
        }
    }

    suspend fun deleteLibraryEntry(id: String) {
        libraryDataStore.save(libraryDataStore.entries.first().filterNot { it.id == id })
    }

    private fun encode(profile: Profile): ByteArray =
        NdefMessageFactory.build(profile.fields, profile.aarPackageName?.let { AarConfig(it) })

    private fun decode(uri: Uri): DecodedNfcEmuFile {
        val text = readText(uri)
        return NfcEmuFileCodec.decode(text)
    }

    private suspend fun recordLibraryEntry(
        profile: Profile,
        uri: Uri,
        direction: LibraryEntryDirection,
        linkedProfileId: String? = null,
    ) {
        val entry = LibraryEntry(
            uri = uri.toString(),
            displayName = queryDisplayName(uri) ?: profile.name,
            profileName = profile.name,
            payloadTypeLabel = profile.fields.typeLabel(),
            direction = direction,
            linkedProfileId = linkedProfileId,
        )
        libraryDataStore.save(libraryDataStore.entries.first() + entry)
    }

    private suspend fun updateLinkedProfileId(entryId: String, profileId: String) {
        libraryDataStore.save(
            libraryDataStore.entries.first().map {
                if (it.id == entryId) it.copy(linkedProfileId = profileId) else it
            },
        )
    }

    private fun writeText(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open target file")
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Could not open target file")
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Could not open file")

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }

    private fun takePersistableReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
