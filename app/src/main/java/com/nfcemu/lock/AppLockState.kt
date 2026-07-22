package com.nfcemu.lock

import com.nfcemu.data.local.SettingsDataStore
import com.nfcemu.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whole-app biometric lock state, opt-in via [SettingsDataStore.requireBiometricUnlock].
 *
 * Deliberately takes no Android `Context` and no `ProcessLifecycleOwner` dependency, so it's
 * constructible and testable in plain JUnit exactly like [com.nfcemu.data.ProfileRepository]
 * (temp-file `DataStore` + a test [CoroutineScope]) - the one piece that actually touches
 * `ProcessLifecycleOwner` is deliberately kept as thin, untested glue in
 * [com.nfcemu.NfcEmuApplication], which calls [onAppBackgrounded].
 *
 * This only ever gates NfcEmu's own UI (profile list, editing, exports) - it cannot and does
 * not stop [com.nfcemu.hce.NfcEmuHostApduService] from continuing to answer NFC readers while
 * locked or backgrounded, since that service reads the active profile's cached NDEF bytes
 * directly, independent of the app's foreground state. See the Settings screen's toggle
 * description, which states this explicitly.
 */
interface AppLockState {
    /** `null` = still reading the persisted setting at process start - avoids flashing the wrong screen. */
    val isLocked: StateFlow<Boolean?>

    /** Called once by the process-lifecycle glue in [com.nfcemu.NfcEmuApplication] whenever the process backgrounds. */
    fun onAppBackgrounded()

    /** Called after a successful biometric/device-credential authentication. */
    fun unlock()

    /** Escape hatch for when the device no longer has any usable biometric/credential configured. */
    fun disableAndUnlock()
}

@Singleton
class DefaultAppLockState @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) : AppLockState {

    private val _isLocked = MutableStateFlow<Boolean?>(null)
    override val isLocked: StateFlow<Boolean?> = _isLocked.asStateFlow()

    @Volatile
    private var lockEnabled = false

    init {
        scope.launch {
            lockEnabled = settingsDataStore.requireBiometricUnlock.first()
            _isLocked.value = lockEnabled
        }
        // Keeps lockEnabled current if the setting is flipped mid-session - this can only
        // happen while already unlocked (the Settings screen is itself gated), so it never
        // needs to force an immediate lock, only affects the *next* backgrounding.
        settingsDataStore.requireBiometricUnlock
            .onEach { lockEnabled = it }
            .launchIn(scope)
    }

    override fun onAppBackgrounded() {
        // Idempotent by construction: harmless if called while already locked (e.g. the
        // device-credential PIN fallback can itself briefly stop the calling Activity on
        // some OEMs/API levels, re-triggering this without it being a relock loop).
        if (lockEnabled) _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }

    override fun disableAndUnlock() {
        _isLocked.value = false
        scope.launch { settingsDataStore.setRequireBiometricUnlock(false) }
    }
}
