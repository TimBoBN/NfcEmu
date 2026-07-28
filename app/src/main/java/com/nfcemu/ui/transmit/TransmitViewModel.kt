package com.nfcemu.ui.transmit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransmitUiState(val activeProfile: Profile? = null) {
    /** True while broadcasting content shared in via the Android share sheet, not a saved profile. */
    val isEphemeralShare: Boolean get() = activeProfile?.id == Profile.SHARED_ID
}

/**
 * Deliberately thin: the *calling* screen (Home's carousel, Profile List's row) already
 * calls [ProfileRepository.setActive] itself before navigating here - this ViewModel never
 * sets anything on entry, it purely observes the already-active profile reactively, mirroring
 * [com.nfcemu.ui.home.HomeViewModel]'s existing `combine` pattern.
 */
@HiltViewModel
class TransmitViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<TransmitUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfileId,
    ) { profiles, activeId ->
        TransmitUiState(profiles.find { it.id == activeId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransmitUiState())

    /**
     * Closing this screen stops emulation - "Transmit open" is the mental model for "actively
     * broadcasting". A share-sheet row that was never saved (see [saveAsProfile]) is discarded
     * outright rather than just deactivated, since it has no place in the profile list.
     */
    fun deactivate() {
        viewModelScope.launch {
            if (uiState.value.isEphemeralShare) profileRepository.discardShared() else profileRepository.clearActive()
        }
    }

    /** Promotes the ephemeral share-sheet row (see [TransmitUiState.isEphemeralShare]) to a real, saved profile. */
    fun saveAsProfile(name: String) {
        viewModelScope.launch { profileRepository.saveSharedAsProfile(name) }
    }
}
