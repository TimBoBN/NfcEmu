package com.nfcemu.data.activity

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nfcemu.di.ActivityStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the recent-activity log as JSON, same pattern as ProfileDataStore/LibraryDataStore. */
@Singleton
class RecentActivityDataStore @Inject constructor(
    @ActivityStore private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val entriesKey = stringPreferencesKey("recent_activity_json")

    val entries: Flow<List<RecentActivityEntry>> = dataStore.data
        .map { prefs ->
            val raw = prefs[entriesKey] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<RecentActivityEntry>>(raw) }.getOrElse { emptyList() }
        }
        .catch { emit(emptyList()) }

    suspend fun save(entries: List<RecentActivityEntry>) {
        dataStore.edit { prefs -> prefs[entriesKey] = json.encodeToString(entries) }
    }
}
