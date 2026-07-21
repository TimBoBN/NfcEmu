package com.nfcemu.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.nfc.NfcHardwareState
import com.nfcemu.ui.components.icon
import com.nfcemu.ui.components.previewText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfiles: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToNewProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewProfile) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_new_profile))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            if (uiState.nfcState != NfcHardwareState.ENABLED) {
                NfcDisabledBanner(
                    state = uiState.nfcState,
                    onOpenSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                )
                Spacer(Modifier.height(16.dp))
            }

            ActiveProfileCard(profile = uiState.activeProfile)

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.home_quick_select), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            QuickSelectRow(
                profiles = uiState.quickSelectProfiles,
                activeProfileId = uiState.activeProfile?.id,
                onSelect = viewModel::selectProfile,
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onNavigateToProfiles) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.home_all_profiles))
                }
                Button(onClick = onNavigateToLibrary) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.home_library))
                }
            }
        }
    }
}

@Composable
private fun NfcDisabledBanner(state: NfcHardwareState, onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = if (state == NfcHardwareState.NOT_SUPPORTED) {
                        stringResource(R.string.home_nfc_not_supported)
                    } else {
                        stringResource(R.string.home_nfc_disabled)
                    },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (state == NfcHardwareState.DISABLED) {
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.home_nfc_open_settings))
                }
            }
        }
    }
}

@Composable
private fun ActiveProfileCard(profile: Profile?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_active_profile),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            if (profile == null) {
                Text(
                    text = stringResource(R.string.home_no_active_profile),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = profile.fields.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            profile.fields.previewText(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSelectRow(
    profiles: List<Profile>,
    activeProfileId: String?,
    onSelect: (String) -> Unit,
) {
    if (profiles.isEmpty()) {
        Text(
            stringResource(R.string.home_quick_select_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(profiles, key = { it.id }) { profile ->
            AssistChip(
                onClick = { onSelect(profile.id) },
                label = { Text(profile.name) },
                leadingIcon = {
                    Icon(profile.fields.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (profile.pinned) {
                    {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.profile_pinned),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else null,
                modifier = Modifier.semantics {
                    contentDescription = if (profile.id == activeProfileId) {
                        "${profile.name}, aktiv"
                    } else {
                        "${profile.name}, antippen um zu aktivieren"
                    }
                },
            )
        }
    }
}
