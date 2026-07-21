package com.nfcemu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.nfc.NfcHardwareState
import com.nfcemu.nfc.NfcStateSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeProfile: Profile? = null,
    val quickSelectProfiles: List<Profile> = emptyList(),
    val nfcState: NfcHardwareState = NfcHardwareState.ENABLED,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    nfcStateSource: NfcStateSource,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfileId,
        nfcStateSource.state,
    ) { profiles, activeId, nfcState ->
        HomeUiState(
            activeProfile = profiles.find { it.id == activeId },
            quickSelectProfiles = quickSelection(profiles, activeId),
            nfcState = nfcState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun quickSelection(profiles: List<Profile>, activeId: String?): List<Profile> {
        val pinned = profiles.filter { it.pinned }
        val recent = profiles
            .filter { !it.pinned && it.id != activeId }
            .sortedByDescending { it.lastUsedAt ?: it.createdAt }
            .take(MAX_RECENT)
        return (pinned + recent).distinctBy { it.id }
    }

    fun selectProfile(id: String) {
        viewModelScope.launch { profileRepository.setActive(id) }
    }

    fun togglePinned(id: String) {
        viewModelScope.launch { profileRepository.togglePinned(id) }
    }

    private companion object {
        const val MAX_RECENT = 8
    }
}
