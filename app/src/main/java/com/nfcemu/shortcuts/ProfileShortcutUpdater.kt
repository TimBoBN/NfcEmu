package com.nfcemu.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.di.ApplicationScope
import com.nfcemu.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the launcher's dynamic app shortcuts in sync with which profiles are pinned.
 * Instantiated eagerly alongside [ProfileRepository] from [com.nfcemu.NfcEmuApplication] -
 * see that class's kdoc for why (a shortcut can be long-pressed into existence, and
 * tapped, without the app UI ever having been opened).
 *
 * `ShortcutManagerCompat` no-ops below API 25 on its own, so no version gating is
 * needed here.
 */
@Singleton
class ProfileShortcutUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    profileRepository: ProfileRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    init {
        profileRepository.profiles
            .onEach { profiles -> ShortcutManagerCompat.setDynamicShortcuts(context, buildShortcuts(profiles)) }
            .launchIn(scope)
    }

    private fun buildShortcuts(profiles: List<Profile>): List<ShortcutInfoCompat> =
        selectShortcutProfiles(profiles).map { profile ->
            ShortcutInfoCompat.Builder(context, "profile-${profile.id}")
                .setShortLabel(profile.name)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_widget_profile))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(EXTRA_ACTIVATE_PROFILE_ID, profile.id)
                    },
                )
                .build()
        }

    companion object {
        const val EXTRA_ACTIVATE_PROFILE_ID = "com.nfcemu.shortcuts.EXTRA_ACTIVATE_PROFILE_ID"
        const val MAX_SHORTCUTS = 4

        /**
         * Which pinned profiles become shortcuts, and in what order. Most-recently-used
         * pinned profiles win when there are more than [MAX_SHORTCUTS] pinned - mirrors
         * [com.nfcemu.ui.home.HomeViewModel]'s recency ordering for the same reason: the
         * profile you're most likely to want next is the one you used most recently.
         */
        fun selectShortcutProfiles(profiles: List<Profile>): List<Profile> =
            profiles.filter { it.pinned }
                .sortedByDescending { it.lastUsedAt ?: it.createdAt }
                .take(MAX_SHORTCUTS)
    }
}
