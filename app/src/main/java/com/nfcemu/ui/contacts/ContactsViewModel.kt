package com.nfcemu.ui.contacts

import androidx.lifecycle.ViewModel
import com.nfcemu.data.contacts.Contact
import com.nfcemu.data.contacts.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    contactsRepository: ContactsRepository,
) : ViewModel() {
    val contacts: StateFlow<List<Contact>> = contactsRepository.contacts
}
