package com.nfcemu.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfiles: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToNewProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNewProfile) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_new_profile))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.md)) {

            AnimatedVisibility(
                visible = uiState.nfcState != NfcHardwareState.ENABLED,
                enter = expandVertically(Motion.standard()) + fadeIn(Motion.standard()),
                exit = shrinkVertically(Motion.standard()) + fadeOut(Motion.standard()),
            ) {
                Column {
                    NfcDisabledBanner(
                        state = uiState.nfcState,
                        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
            }

            ActiveProfileCard(profile = uiState.activeProfile, onDeactivate = viewModel::deactivate)

            Spacer(Modifier.height(Spacing.lg))

            Text(stringResource(R.string.home_quick_select), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            QuickSelectRow(
                profiles = uiState.quickSelectProfiles,
                activeProfileId = uiState.activeProfile?.id,
                onSelect = viewModel::selectProfile,
            )

            Spacer(Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilledTonalButton(onClick = onNavigateToProfiles) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.size(Spacing.sm))
                    Text(stringResource(R.string.home_all_profiles))
                }
                FilledTonalButton(onClick = onNavigateToLibrary) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.size(Spacing.sm))
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
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.size(Spacing.sm + Spacing.xs))
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
                FilledTonalButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.home_nfc_open_settings))
                }
            }
        }
    }
}

@Composable
private fun ActiveProfileCard(profile: Profile?, onDeactivate: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_active_profile),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.sm + Spacing.xs))
            AnimatedContent(
                targetState = profile,
                transitionSpec = {
                    (fadeIn(Motion.standard()) togetherWith fadeOut(tween(Motion.DURATION_SHORT)))
                },
                label = "active-profile",
            ) { targetProfile ->
                if (targetProfile == null) {
                    Text(
                        text = stringResource(R.string.home_no_active_profile),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = targetProfile.fields.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(targetProfile.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                targetProfile.fields.previewText(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onDeactivate) {
                            Text(stringResource(R.string.action_deactivate))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickSelectRow(
    profiles: List<Profile>,
    activeProfileId: String?,
    onSelect: (String) -> Unit,
) {
    if (profiles.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Inbox,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                stringResource(R.string.home_quick_select_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                modifier = Modifier.animateItemPlacement().semantics {
                    contentDescription = if (profile.id == activeProfileId) {
                        "${profile.name}, active"
                    } else {
                        "${profile.name}, tap to activate"
                    }
                },
            )
        }
    }
}
