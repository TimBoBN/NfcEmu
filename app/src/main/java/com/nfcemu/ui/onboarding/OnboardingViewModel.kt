package com.nfcemu.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    /** Null while the persisted flag hasn't loaded yet, to avoid flashing the onboarding screen. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsDataStore.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        viewModelScope.launch { settingsDataStore.setOnboardingCompleted() }
    }
}
