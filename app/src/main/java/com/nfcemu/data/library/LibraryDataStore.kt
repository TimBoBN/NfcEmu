package com.nfcemu.data.library

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nfcemu.di.LibraryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the "Gespeicherte Dateien" index (see [LibraryEntry]) as JSON, same pattern as ProfileDataStore. */
@Singleton
class LibraryDataStore @Inject constructor(
    @LibraryStore private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val entriesKey = stringPreferencesKey("entries_json")

    val entries: Flow<List<LibraryEntry>> = dataStore.data
        .map { prefs ->
            val raw = prefs[entriesKey] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<LibraryEntry>>(raw) }.getOrElse { emptyList() }
        }
        .catch { emit(emptyList()) }

    suspend fun save(entries: List<LibraryEntry>) {
        dataStore.edit { prefs -> prefs[entriesKey] = json.encodeToString(entries) }
    }
}
