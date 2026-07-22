package com.nfcemu.ui.profilelist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.FileRepository
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileListUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: String? = null,
)

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileListUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfileId,
    ) { profiles, activeId ->
        val visibleProfiles = profiles.filterNot { it.id == Profile.MY_PROFILE_ID }
        ProfileListUiState(profiles = visibleProfiles.sortedByDescending { it.pinned }, activeProfileId = activeId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileListUiState())

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    fun setActive(id: String) {
        viewModelScope.launch { profileRepository.setActive(id) }
    }

    fun deactivate() {
        viewModelScope.launch { profileRepository.clearActive() }
    }

    fun duplicate(id: String) {
        viewModelScope.launch { profileRepository.duplicateProfile(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { profileRepository.deleteProfile(id) }
    }

    fun togglePinned(id: String) {
        viewModelScope.launch { profileRepository.togglePinned(id) }
    }

    fun exportProfile(profile: Profile, targetUri: Uri, rawNdefOnly: Boolean) {
        viewModelScope.launch {
            val result = if (rawNdefOnly) {
                fileRepository.exportRawNdef(profile, targetUri)
            } else {
                fileRepository.exportProfile(profile, targetUri)
            }
            result.onFailure { _exportError.value = it.message ?: "Export failed" }
        }
    }

    fun exportAllAsZip(targetUri: Uri) {
        viewModelScope.launch {
            val exportable = profileRepository.profiles.value.filterNot { it.id == Profile.MY_PROFILE_ID }
            fileRepository.exportLibraryAsZip(exportable, targetUri)
                .onFailure { _exportError.value = it.message ?: "Export failed" }
        }
    }

    fun clearExportError() {
        _exportError.value = null
    }
}
