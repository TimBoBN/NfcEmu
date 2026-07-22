package com.nfcemu.lock

import android.content.Context
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BiometricAvailability { AVAILABLE, NOT_CONFIGURED, UNSUPPORTED }

/** Narrow, mockable contract around [BiometricManager], same pattern as [com.nfcemu.nfc.NfcStateSource]. */
interface BiometricAvailabilitySource {
    fun currentAvailability(): BiometricAvailability
}

@Singleton
class BiometricManagerAvailabilitySource @Inject constructor(
    @ApplicationContext private val context: Context,
) : BiometricAvailabilitySource {

    override fun currentAvailability(): BiometricAvailability {
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (BiometricManager.from(context).canAuthenticate(allowed)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_CONFIGURED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }
}
