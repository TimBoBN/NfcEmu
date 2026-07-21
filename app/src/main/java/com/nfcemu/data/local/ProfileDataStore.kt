package com.nfcemu.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nfcemu.data.Profile
import com.nfcemu.di.ProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the full profile library + which one is active in a single Preferences
 * DataStore entry (JSON-encoded list). Profile counts are small (user-authored
 * emulation profiles, not a bulk dataset), so one JSON blob per key is simpler than a
 * Proto DataStore schema while still giving us Flow-based, non-blocking reads.
 */
@Singleton
class ProfileDataStore @Inject constructor(
    @ProfileStore private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val profilesKey = stringPreferencesKey("profiles_json")
    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")

    val profiles: Flow<List<Profile>> = dataStore.data
        .map { prefs ->
            val raw = prefs[profilesKey] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<Profile>>(raw) }.getOrElse { emptyList() }
        }
        .catch { emit(emptyList()) }

    val activeProfileId: Flow<String?> = dataStore.data
        .map { prefs -> prefs[activeProfileIdKey] }
        .catch { emit(null) }

    suspend fun saveProfiles(profiles: List<Profile>) {
        val encoded = json.encodeToString(profiles)
        dataStore.edit { prefs -> prefs[profilesKey] = encoded }
    }

    suspend fun saveActiveProfileId(id: String?) {
        dataStore.edit { prefs ->
            if (id == null) prefs.remove(activeProfileIdKey) else prefs[activeProfileIdKey] = id
        }
    }
}
