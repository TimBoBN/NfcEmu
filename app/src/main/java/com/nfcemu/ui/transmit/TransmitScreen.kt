package com.nfcemu.ui.transmit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.ui.components.NfcEmuCard
import com.nfcemu.ui.components.NfcEmuSecondaryButton
import com.nfcemu.ui.components.TypeIconBadge
import com.nfcemu.ui.components.previewText
import com.nfcemu.ui.components.typeDisplayLabel
import com.nfcemu.ui.components.typeGlyph
import com.nfcemu.ui.theme.Spacing

/**
 * Full-screen "actively broadcasting" confirmation. The calling screen (Home's carousel,
 * Profile List's row) already sets the active profile before navigating here - this screen
 * only observes it reactively. Both the close icon and the "Done" button stop emulation
 * before leaving: "Transmit open" is the mental model for "actively broadcasting", not a
 * dismissible confirmation toast.
 */
@Composable
fun TransmitScreen(
    onClose: () -> Unit,
    viewModel: TransmitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val close: () -> Unit = {
        viewModel.deactivate()
        onClose()
    }
    BackHandler(onBack = close)
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    if (showSaveDialog) {
        SaveAsProfileDialog(
            initialName = uiState.activeProfile?.name.orEmpty(),
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.saveAsProfile(name)
                showSaveDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.sm), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = close) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_nocturne_close),
                        contentDescription = stringResource(R.string.transmit_done),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        PulseRings()
                        uiState.activeProfile?.let { profile -> TransmitCard(profile) }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Text(stringResource(R.string.transmit_ready), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(R.string.transmit_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                    )
                }
            }
            if (uiState.isEphemeralShare) {
                NfcEmuSecondaryButton(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.transmit_save_as_profile))
                }
                Spacer(Modifier.height(Spacing.sm))
            }
            NfcEmuSecondaryButton(onClick = close, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.transmit_done))
            }
        }
    }
}

@Composable
private fun SaveAsProfileDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transmit_save_as_profile)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.profile_form_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TransmitCard(profile: Profile) {
    NfcEmuCard {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeIconBadge(profile.fields.typeGlyph(), size = 32.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    profile.fields.typeDisplayLabel().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            Text(
                profile.fields.previewText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Three staggered expanding/fading rings sharing one period - mirrors the mockup's
 * `@keyframes wavesRing` with staggered `animation-delay`. Each ring keeps the same 2200ms
 * period; [StartOffset] only offsets its phase so the stagger holds indefinitely instead of
 * drifting the way giving each ring its own, longer, delay-inclusive duration would.
 */
@Composable
private fun PulseRings(modifier: Modifier = Modifier, baseSize: Dp = 140.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "transmit-rings")
    Box(modifier = modifier.size(baseSize * 2.2f), contentAlignment = Alignment.Center) {
        repeat(3) { i ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(i * 700),
                ),
                label = "ring-$i",
            )
            Box(
                modifier = Modifier
                    .size(baseSize * (1f + 1.2f * progress))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f * (1f - progress)), CircleShape),
            )
        }
    }
}
