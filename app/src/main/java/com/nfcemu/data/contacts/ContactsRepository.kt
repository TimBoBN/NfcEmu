package com.nfcemu.data.contacts

import com.nfcemu.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    private val dataStore: ContactsDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val contacts: StateFlow<List<Contact>> = dataStore.contacts
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Reads the DataStore Flow directly (not [contacts]) - see ProfileRepository's kdoc for why. */
    suspend fun add(contact: Contact) {
        dataStore.save(listOf(contact) + dataStore.contacts.first())
    }
}
