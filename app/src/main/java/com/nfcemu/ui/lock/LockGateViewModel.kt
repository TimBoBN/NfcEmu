package com.nfcemu.ui.lock

import androidx.lifecycle.ViewModel
import com.nfcemu.lock.AppLockState
import com.nfcemu.lock.BiometricAvailabilitySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Thin pass-through so Compose can obtain [AppLockState] via `hiltViewModel()` like every other screen. */
@HiltViewModel
class LockGateViewModel @Inject constructor(
    private val appLockState: AppLockState,
    private val biometricAvailabilitySource: BiometricAvailabilitySource,
) : ViewModel() {

    val isLocked: StateFlow<Boolean?> = appLockState.isLocked

    fun onUnlocked() = appLockState.unlock()

    fun onDisableAndUnlock() = appLockState.disableAndUnlock()

    // Re-queried at render time by LockScreen rather than cached here - a device's screen
    // lock can only be added/removed while the user has left the app, so a stale cached
    // value from long before this composition would be wrong.
    fun currentBiometricAvailability() = biometricAvailabilitySource.currentAvailability()
}
