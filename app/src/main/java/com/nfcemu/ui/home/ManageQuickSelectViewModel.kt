package com.nfcemu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageQuickSelectUiState(
    val selected: List<Profile> = emptyList(),
    val available: List<Profile> = emptyList(),
)

/** Backs the dedicated screen for curating Home's quick-select carousel - see [HomeViewModel]. */
@HiltViewModel
class ManageQuickSelectViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<ManageQuickSelectUiState> = profileRepository.profiles
        .map { profiles ->
            val visible = profiles.filterNot { it.id in Profile.RESERVED_IDS }
            ManageQuickSelectUiState(
                selected = visible.filter { it.quickSelectOrder != null }.sortedBy { it.quickSelectOrder },
                available = visible.filterNot { it.quickSelectOrder != null }.sortedBy { it.name.lowercase() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageQuickSelectUiState())

    fun add(id: String) {
        viewModelScope.launch { profileRepository.addToQuickSelect(id) }
    }

    fun remove(id: String) {
        viewModelScope.launch { profileRepository.removeFromQuickSelect(id) }
    }

    /** [orderedIds] must be exactly [ManageQuickSelectUiState.selected]'s ids, reordered - see [ProfileRepository.reorderQuickSelect]. */
    fun reorder(orderedIds: List<String>) {
        viewModelScope.launch { profileRepository.reorderQuickSelect(orderedIds) }
    }
}
