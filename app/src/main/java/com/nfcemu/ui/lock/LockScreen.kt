package com.nfcemu.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.nfcemu.R
import com.nfcemu.lock.BiometricAvailability
import com.nfcemu.ui.theme.Spacing
import com.nfcemu.ui.util.findActivity

/** Full-screen, no back navigation - there is nothing to go back to while locked. */
@Composable
fun LockScreen(viewModel: LockGateViewModel) {
    val activity = LocalContext.current.findActivity() as? FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val availability = viewModel.currentBiometricAvailability()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer()
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
            Spacer()

            if (availability == BiometricAvailability.AVAILABLE && activity != null) {
                Text(
                    stringResource(R.string.lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer()
                val launchPrompt = rememberBiometricPromptLauncher(
                    activity = activity,
                    title = stringResource(R.string.lock_title),
                    onSuccess = viewModel::onUnlocked,
                    onError = { message -> errorMessage = message },
                )
                Button(onClick = launchPrompt) {
                    Text(stringResource(R.string.lock_unlock_button))
                }
                errorMessage?.let { message ->
                    Spacer()
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // The device itself no longer has a screen lock or biometric configured. Its
                // own lock screen already provides zero protection in that state, so refusing
                // to ever let the user back in (there is no way to authenticate them) would be
                // strictly worse than degrading to "no app lock" and explaining why.
                Text(
                    stringResource(R.string.lock_no_credential_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer()
                TextButton(onClick = viewModel::onDisableAndUnlock) {
                    Text(stringResource(R.string.lock_disable_button))
                }
            }
        }
    }
}

@Composable
private fun Spacer() = androidx.compose.foundation.layout.Spacer(Modifier.padding(top = Spacing.sm + Spacing.xs))
