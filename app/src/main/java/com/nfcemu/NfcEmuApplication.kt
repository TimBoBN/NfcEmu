package com.nfcemu

import android.app.Application
import com.nfcemu.data.ProfileRepository
import com.nfcemu.widget.ProfileWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The HCE service (or a home screen widget tap) can be the very first thing the
 * system starts in this process - the user may never have opened the app UI.
 * Field-injecting these singletons here forces Hilt to construct them (and start
 * their DataStore-backed Flows) as soon as the process exists, instead of lazily on
 * first UI access:
 * - [ProfileRepository]: so the emulated card is never empty just because the
 *   Activity hasn't run yet.
 * - [ProfileWidgetUpdater]: so the home screen widget keeps refreshing even if the
 *   user never opens the app after placing it.
 */
@HiltAndroidApp
class NfcEmuApplication : Application() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var profileWidgetUpdater: ProfileWidgetUpdater
}
