package com.nfcemu.ui.receivecontact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.ui.components.NfcEmuCard
import com.nfcemu.ui.components.NfcEmuPrimaryButton
import com.nfcemu.ui.components.NfcEmuSecondaryButton
import com.nfcemu.ui.components.PulsingIconCircle
import com.nfcemu.ui.theme.Spacing
import com.nfcemu.ui.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveContactScreen(
    onBack: () -> Unit,
    viewModel: ReceiveContactViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity != null) viewModel.startScanning(activity)
        onDispose {
            if (activity != null) viewModel.stopScanning(activity)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receive_contact_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                is ReceiveContactUiState.Unsupported -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nocturne_nfc),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer()
                    Text(stringResource(R.string.receive_contact_error_title), style = MaterialTheme.typography.titleMedium)
                    Spacer()
                    Text(
                        state.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer()
                    NfcEmuPrimaryButton(onClick = viewModel::dismissResult) {
                        Text(stringResource(R.string.scan_tag_try_again))
                    }
                }
                is ReceiveContactUiState.Scanned -> {
                    NfcEmuCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text(state.vcard.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                            state.vcard.phones.firstOrNull()?.let {
                                Spacer()
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.vcard.emails.firstOrNull()?.let {
                                Spacer()
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            state.vcard.organization?.let {
                                Spacer()
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer()
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        NfcEmuSecondaryButton(onClick = onBack) {
                            Text(stringResource(R.string.receive_contact_discard))
                        }
                        NfcEmuPrimaryButton(onClick = { viewModel.saveContact(); onBack() }) {
                            Text(stringResource(R.string.receive_contact_save))
                        }
                    }
                }
                ReceiveContactUiState.Waiting -> {
                    PulsingIconCircle(icon = ImageVector.vectorResource(R.drawable.ic_nocturne_nfc), size = 80.dp)
                    Spacer()
                    Text(
                        stringResource(R.string.receive_contact_waiting),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun Spacer() = androidx.compose.foundation.layout.Spacer(Modifier.padding(top = Spacing.sm + Spacing.xs))
