package com.nfcemu.ui.profileform

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nfcemu.R
import com.nfcemu.ui.components.icon
import com.nfcemu.ui.components.label
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypePickerScreen(
    onBack: () -> Unit,
    onTypeSelected: (ProfileTypeTemplate) -> Unit,
    onScanTag: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.type_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).padding(Spacing.sm + Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
            ) {
                items(ProfileTypeTemplate.entries) { template ->
                    TypeTile(template = template, onClick = { onTypeSelected(template) })
                }
            }
            OutlinedButton(
                onClick = onScanTag,
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            ) {
                Icon(Icons.Filled.Nfc, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.padding(start = Spacing.xs))
                Text(stringResource(R.string.type_picker_scan_tag))
            }
        }
    }
}

@Composable
private fun TypeTile(template: ProfileTypeTemplate, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = Motion.standard(Motion.DURATION_SHORT),
        label = "tile-press-scale",
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .semantics(mergeDescendants = true) {
                contentDescription = template.label()
                role = Role.Button
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.sm + Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = template.icon(),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = template.label(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
