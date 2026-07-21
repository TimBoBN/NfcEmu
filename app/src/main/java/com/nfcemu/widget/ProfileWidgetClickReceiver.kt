package com.nfcemu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nfcemu.data.ProfileRepository
import com.nfcemu.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Target of the widget list's `pendingIntentTemplate` (see [ProfileWidgetProvider]):
 * each row's fill-in intent carries [EXTRA_PROFILE_ID], merged into this by Android
 * when the row is tapped. Kept as a plain broadcast receiver (not a service or
 * activity) since activating a profile is just a quick DataStore write.
 */
@AndroidEntryPoint
class ProfileWidgetClickReceiver : BroadcastReceiver() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: return
        val pendingResult = goAsync()
        scope.launch {
            try {
                profileRepository.setActive(profileId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "com.nfcemu.widget.EXTRA_PROFILE_ID"
    }
}
