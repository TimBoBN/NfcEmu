package com.nfcemu.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Builds a [BiometricPrompt]/[BiometricPrompt.PromptInfo] pair once per [activity] via
 * [remember], and returns a function that triggers it. Allows device-credential (PIN/pattern)
 * fallback so the lock still works on devices without biometric hardware enrolled.
 *
 * Important: [BiometricPrompt.PromptInfo.Builder.setNegativeButtonText] must **not** be called
 * here - combined with `DEVICE_CREDENTIAL` in [BiometricPrompt.PromptInfo.Builder.setAllowedAuthenticators],
 * that throws [IllegalArgumentException] at runtime (the system supplies its own "use PIN
 * instead" affordance). Cancelling the prompt just leaves the user on [LockScreen], which is
 * the correct fallback - no negative-button callback is needed.
 */
@Composable
fun rememberBiometricPromptLauncher(
    activity: FragmentActivity,
    title: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val onSuccessState = rememberUpdatedState(onSuccess)
    val onErrorState = rememberUpdatedState(onError)

    val prompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccessState.value()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onErrorState.value(errString.toString())
                }

                // onAuthenticationFailed (wrong finger/face) intentionally left unhandled -
                // the system dialog already shows its own "not recognized" feedback and stays
                // open, no app-level action needed.
            },
        )
    }

    val promptInfo = remember(title) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    return remember(prompt, promptInfo) { { prompt.authenticate(promptInfo) } }
}
