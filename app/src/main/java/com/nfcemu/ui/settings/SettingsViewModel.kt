package com.nfcemu.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.local.SettingsDataStore
import com.nfcemu.lock.BiometricAvailability
import com.nfcemu.lock.BiometricAvailabilitySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val requireBiometricUnlock: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val biometricAvailabilitySource: BiometricAvailabilitySource,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsDataStore.requireBiometricUnlock
        .map { enabled -> SettingsUiState(enabled, biometricAvailabilitySource.currentAvailability()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    /** No-ops (rather than persisting) if biometric/device-credential isn't configured - the switch is also disabled in the UI, this is defense-in-depth. */
    fun setRequireBiometricUnlock(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && biometricAvailabilitySource.currentAvailability() != BiometricAvailability.AVAILABLE) return@launch
            settingsDataStore.setRequireBiometricUnlock(enabled)
        }
    }
}
