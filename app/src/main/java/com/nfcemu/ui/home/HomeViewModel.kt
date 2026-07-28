package com.nfcemu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.data.activity.RecentActivityEntry
import com.nfcemu.data.activity.RecentActivityRepository
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
    val recentActivity: List<RecentActivityEntry> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    nfcStateSource: NfcStateSource,
    recentActivityRepository: RecentActivityRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfileId,
        nfcStateSource.state,
        recentActivityRepository.recent,
    ) { profiles, activeId, nfcState, recentActivity ->
        val visibleProfiles = profiles.filterNot { it.id in Profile.RESERVED_IDS }
        HomeUiState(
            activeProfile = profiles.find { it.id == activeId },
            quickSelectProfiles = visibleProfiles
                .filter { it.quickSelectOrder != null }
                .sortedBy { it.quickSelectOrder },
            nfcState = nfcState,
            recentActivity = recentActivity,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun selectProfile(id: String) {
        viewModelScope.launch { profileRepository.setActive(id) }
    }
}
