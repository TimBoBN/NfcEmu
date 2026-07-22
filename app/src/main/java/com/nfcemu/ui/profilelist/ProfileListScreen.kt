package com.nfcemu.ui.profilelist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.ui.components.NfcEmuCard
import com.nfcemu.ui.components.NfcEmuOutlinedFab
import com.nfcemu.ui.components.TypeIconBadge
import com.nfcemu.ui.components.previewText
import com.nfcemu.ui.components.typeGlyph
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileListScreen(
    onBack: () -> Unit,
    onNewProfile: () -> Unit,
    onEditProfile: (Profile) -> Unit,
    onWriteToTag: (Profile) -> Unit,
    onNavigateToTransmit: () -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var profilePendingDelete by remember { mutableStateOf<Profile?>(null) }
    var profilePendingExport by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExportRaw by rememberSaveable { mutableStateOf(false) }
    var topBarMenuExpanded by remember { mutableStateOf(false) }

    val nfcemuLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val profile = uiState.profiles.find { it.id == profilePendingExport }
        if (uri != null && profile != null) {
            viewModel.exportProfile(profile, uri, rawNdefOnly = pendingExportRaw)
        }
        profilePendingExport = null
    }
    val zipExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportAllAsZip)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { topBarMenuExpanded = true }) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_more_vert), contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = topBarMenuExpanded, onDismissRequest = { topBarMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export_all_zip)) },
                            onClick = {
                                topBarMenuExpanded = false
                                zipExportLauncher.launch("NfcEmu-profiles.zip")
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            NfcEmuOutlinedFab(
                onClick = onNewProfile,
                icon = ImageVector.vectorResource(R.drawable.ic_nocturne_add),
                contentDescription = stringResource(R.string.home_new_profile),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(uiState.profiles, key = { it.id }) { profile ->
                ProfileRow(
                    profile = profile,
                    isActive = profile.id == uiState.activeProfileId,
                    onSetActive = { viewModel.setActive(profile.id) },
                    onDeactivate = viewModel::deactivate,
                    onOpenTransmit = { viewModel.setActive(profile.id); onNavigateToTransmit() },
                    onEdit = { onEditProfile(profile) },
                    onWriteToTag = { onWriteToTag(profile) },
                    onDuplicate = { viewModel.duplicate(profile.id) },
                    onTogglePin = { viewModel.togglePinned(profile.id) },
                    onDelete = { profilePendingDelete = profile },
                    onExport = { raw ->
                        profilePendingExport = profile.id
                        pendingExportRaw = raw
                        val extension = if (raw) "ndef" else "nfcemu"
                        nfcemuLauncher.launch("${profile.name}.$extension")
                    },
                    modifier = Modifier.animateItemPlacement(),
                )
            }
        }
    }

    profilePendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profilePendingDelete = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_message, profile.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile.id)
                    profilePendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { profilePendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDeactivate: () -> Unit,
    onOpenTransmit: () -> Unit,
    onEdit: () -> Unit,
    onWriteToTag: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onExport: (raw: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = Motion.standard(),
        label = "profile-row-highlight",
    )

    NfcEmuCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenTransmit,
        containerColor = containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm + Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeIconBadge(profile.fields.typeGlyph(), size = 36.dp)
            Spacer(Modifier.size(Spacing.sm + Spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    if (profile.pinned) {
                        Spacer(Modifier.size(6.dp))
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_nocturne_pin),
                            contentDescription = stringResource(R.string.profile_pinned),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    profile.fields.previewText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_nocturne_active_check),
                    contentDescription = stringResource(R.string.profile_active),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(Spacing.sm))
            }
            Column {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_more_vert), contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isActive) stringResource(R.string.action_deactivate) else stringResource(R.string.action_set_active),
                            )
                        },
                        onClick = { menuExpanded = false; if (isActive) onDeactivate() else onSetActive() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_write_to_tag)) },
                        onClick = { menuExpanded = false; onWriteToTag() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate)) },
                        onClick = { menuExpanded = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (profile.pinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin)) },
                        onClick = { menuExpanded = false; onTogglePin() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export_nfcemu)) },
                        onClick = { menuExpanded = false; onExport(false) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export_raw)) },
                        onClick = { menuExpanded = false; onExport(true) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}
