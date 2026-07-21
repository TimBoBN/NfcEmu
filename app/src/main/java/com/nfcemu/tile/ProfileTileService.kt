package com.nfcemu.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile that cycles through *pinned* profiles on tap (Android only
 * supports one custom tile per declared TileService, not one per user-defined
 * profile, so cycling is the closest fit to "quick panel entries for profiles").
 * The tile's label always reflects whichever profile is currently active.
 */
@AndroidEntryPoint
class ProfileTileService : TileService() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val profiles = profileRepository.profiles.value
        val pinned = profiles.filter { it.pinned }
        if (pinned.isEmpty()) {
            Toast.makeText(this, getString(R.string.tile_no_pinned_profiles), Toast.LENGTH_SHORT).show()
            return
        }
        val activeId = profileRepository.activeProfileId.value
        val currentIndex = pinned.indexOfFirst { it.id == activeId }
        val next = pinned[(currentIndex + 1).mod(pinned.size)]

        scope.launch {
            profileRepository.setActive(next.id)
            refreshTile(activeOverride = next)
        }
    }

    private fun refreshTile(activeOverride: Profile? = null) {
        val tile = qsTile ?: return
        val active = activeOverride ?: profileRepository.profiles.value.find { it.id == profileRepository.activeProfileId.value }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = active?.name ?: getString(R.string.tile_no_active)
        }
        tile.state = if (active != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
