package com.nfcemu.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.nfc.NfcHardwareState
import com.nfcemu.ui.components.NfcEmuCard
import com.nfcemu.ui.components.NfcEmuOutlinedFab
import com.nfcemu.ui.components.NfcEmuSecondaryButton
import com.nfcemu.ui.components.TypeIconBadge
import com.nfcemu.ui.components.previewText
import com.nfcemu.ui.components.typeGlyph
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfiles: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToNewProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTransmit: () -> Unit,
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
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_nocturne_settings),
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            NfcEmuOutlinedFab(
                onClick = onNavigateToNewProfile,
                icon = ImageVector.vectorResource(R.drawable.ic_nocturne_add),
                contentDescription = stringResource(R.string.home_new_profile),
            )
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

            Text(stringResource(R.string.home_quick_select), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            QuickSelectCarousel(
                profiles = uiState.quickSelectProfiles,
                onSelect = { id -> viewModel.selectProfile(id); onNavigateToTransmit() },
            )

            Spacer(Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                NfcEmuSecondaryButton(onClick = onNavigateToProfiles) {
                    Text(stringResource(R.string.home_all_profiles))
                }
                NfcEmuSecondaryButton(onClick = onNavigateToLibrary) {
                    Text(stringResource(R.string.home_library))
                }
            }
        }
    }
}

@Composable
private fun NfcDisabledBanner(state: NfcHardwareState, onOpenSettings: () -> Unit) {
    NfcEmuCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_nocturne_nfc),
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
                NfcEmuSecondaryButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.home_nfc_open_settings))
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun QuickSelectCarousel(profiles: List<Profile>, onSelect: (String) -> Unit) {
    if (profiles.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_nocturne_import),
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

    val pagerState = rememberPagerState(pageCount = { profiles.size })
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 40.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val profile = profiles[page]
        QuickSelectCard(profile = profile, onFrontTap = { onSelect(profile.id) })
    }
    if (profiles.size > 1) {
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(profiles.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}

@Composable
private fun QuickSelectCard(profile: Profile, onFrontTap: () -> Unit) {
    NfcEmuCard(onClick = onFrontTap, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeIconBadge(profile.fields.typeGlyph(), size = 48.dp)
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    profile.fields.previewText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
