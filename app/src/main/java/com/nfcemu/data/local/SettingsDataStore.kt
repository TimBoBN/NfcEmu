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

/** Small app-wide flags, currently just whether the one-time onboarding has been shown. */
@Singleton
class SettingsDataStore @Inject constructor(
    @SettingsStore private val dataStore: DataStore<Preferences>,
) {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[onboardingCompletedKey] ?: false }
        .catch { emit(false) }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs -> prefs[onboardingCompletedKey] = true }
    }
}
