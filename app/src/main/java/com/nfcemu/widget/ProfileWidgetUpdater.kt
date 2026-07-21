package com.nfcemu.widget

import android.content.Context
import com.nfcemu.data.ProfileRepository
import com.nfcemu.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the home screen widget in sync with the profile library. Instantiated
 * eagerly alongside [ProfileRepository] from [com.nfcemu.NfcEmuApplication] - see
 * that class's kdoc for why eager instantiation matters here too (a widget can be
 * tapped, and needs to be current, without the app UI ever having been opened).
 *
 * Refreshes on *any* change to the profile list or the active id - not just when the
 * active profile's name changes - since pinning/renaming/deleting a profile can
 * change what the widget's row list should show even when the active one doesn't.
 */
@Singleton
class ProfileWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    profileRepository: ProfileRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    init {
        combine(profileRepository.profiles, profileRepository.activeProfileId) { profiles, activeId ->
            profiles.find { it.id == activeId }?.name
        }
            .onEach { activeName -> ProfileWidgetProvider.refreshAll(context, activeName) }
            .launchIn(scope)
    }
}
