package com.nfcemu.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.FileRepository
import com.nfcemu.data.library.LibraryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val fileRepository: FileRepository,
) : ViewModel() {

    val entries: StateFlow<List<LibraryEntry>> = fileRepository.libraryEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            fileRepository.importProfile(uri)
                .onSuccess { _message.value = "“${it.name}” was imported as a new profile." }
                .onFailure { _message.value = it.message ?: "Import failed" }
        }
    }

    fun loadAsActive(entry: LibraryEntry) {
        viewModelScope.launch {
            fileRepository.loadEntryAsActiveProfile(entry)
                .onSuccess { _message.value = "“${entry.profileName}” is now active." }
                .onFailure { _message.value = it.message ?: "Loading failed" }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { fileRepository.deleteLibraryEntry(id) }
    }

    fun clearMessage() {
        _message.value = null
    }
}
