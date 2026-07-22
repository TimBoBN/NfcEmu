package com.nfcemu.ui.writetag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.nfcemu.ui.components.NfcEmuPrimaryButton
import com.nfcemu.ui.components.NfcEmuSecondaryButton
import com.nfcemu.ui.components.PulsingIconCircle
import com.nfcemu.ui.theme.Spacing
import com.nfcemu.ui.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteTagScreen(
    onBack: () -> Unit,
    onWritten: () -> Unit,
    viewModel: WriteTagViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity != null) viewModel.startWriting(activity)
        onDispose {
            if (activity != null) viewModel.stopWriting(activity)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.write_tag_title)) },
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
                is WriteTagUiState.ProfileNotFound -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nocturne_nfc),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer()
                    Text(
                        stringResource(R.string.write_tag_profile_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer()
                    NfcEmuPrimaryButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                }
                is WriteTagUiState.Failure -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nocturne_nfc),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer()
                    Text(stringResource(R.string.write_tag_error_title), style = MaterialTheme.typography.titleMedium)
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
                is WriteTagUiState.Success -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nocturne_active_check),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer()
                    Text(
                        stringResource(R.string.write_tag_success_message, viewModel.profileName),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                    Spacer()
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        NfcEmuSecondaryButton(onClick = viewModel::dismissResult) {
                            Text(stringResource(R.string.write_tag_write_another))
                        }
                        NfcEmuPrimaryButton(onClick = onWritten) {
                            Text(stringResource(R.string.write_tag_done))
                        }
                    }
                }
                is WriteTagUiState.Waiting -> {
                    PulsingIconCircle(icon = ImageVector.vectorResource(R.drawable.ic_nocturne_nfc), size = 80.dp)
                    Spacer()
                    Text(
                        stringResource(R.string.write_tag_waiting, viewModel.profileName),
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
