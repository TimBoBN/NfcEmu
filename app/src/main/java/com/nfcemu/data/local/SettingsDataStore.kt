package com.nfcemu.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nfcemu.di.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Small app-wide flags: onboarding shown, and whether the app requires biometric unlock. */
@Singleton
class SettingsDataStore @Inject constructor(
    @SettingsStore private val dataStore: DataStore<Preferences>,
) {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val requireBiometricUnlockKey = booleanPreferencesKey("require_biometric_unlock")

    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[onboardingCompletedKey] ?: false }
        .catch { emit(false) }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs -> prefs[onboardingCompletedKey] = true }
    }

    /** Off by default - the lock is opt-in, toggled from the Settings screen. */
    val requireBiometricUnlock: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[requireBiometricUnlockKey] ?: false }
        .catch { emit(false) }

    suspend fun setRequireBiometricUnlock(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[requireBiometricUnlockKey] = enabled }
    }
}
