package com.nfcemu.data.contacts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nfcemu.di.ContactsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Persists received contacts as JSON, same pattern as ProfileDataStore/LibraryDataStore. */
@Singleton
class ContactsDataStore @Inject constructor(
    @ContactsStore private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val contactsKey = stringPreferencesKey("contacts_json")

    val contacts: Flow<List<Contact>> = dataStore.data
        .map { prefs ->
            val raw = prefs[contactsKey] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<Contact>>(raw) }.getOrElse { emptyList() }
        }
        .catch { emit(emptyList()) }

    suspend fun save(contacts: List<Contact>) {
        dataStore.edit { prefs -> prefs[contactsKey] = json.encodeToString(contacts) }
    }
}
