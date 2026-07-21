package com.nfcemu

import android.app.Application
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The HCE service can be the very first thing the system starts in this process - the
 * user may tap their phone to a reader without ever having opened the app UI. Field-
 * injecting [ProfileRepository] here forces Hilt to construct that singleton (and
 * start its DataStore-backed active-profile Flow) as soon as the process exists,
 * instead of lazily on first UI access, so the emulated card is never empty just
 * because the Activity hasn't run yet.
 */
@HiltAndroidApp
class NfcEmuApplication : Application() {

    @Inject
    lateinit var profileRepository: ProfileRepository
}
