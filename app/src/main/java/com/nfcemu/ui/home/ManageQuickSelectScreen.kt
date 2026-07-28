package com.nfcemu.ui.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.Profile
import com.nfcemu.ui.components.NfcEmuCard
import com.nfcemu.ui.components.TypeIconBadge
import com.nfcemu.ui.components.typeGlyph
import com.nfcemu.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Curates Home's quick-select carousel: which profiles show up there and in what order.
 * Fully manual, on purpose - see [HomeViewModel.uiState]'s `quickSelectProfiles`, which just
 * reads [Profile.quickSelectOrder] back out, no automatic "recently used" fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQuickSelectScreen(
    onBack: () -> Unit,
    viewModel: ManageQuickSelectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_quick_select_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.md).verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.manage_quick_select_selected_header), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            if (uiState.selected.isEmpty()) {
                Text(
                    stringResource(R.string.manage_quick_select_selected_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SelectedList(
                    items = uiState.selected,
                    onReordered = viewModel::reorder,
                    onRemove = viewModel::remove,
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(stringResource(R.string.manage_quick_select_available_header), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            if (uiState.available.isEmpty()) {
                Text(
                    stringResource(R.string.manage_quick_select_available_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    uiState.available.forEach { profile ->
                        AvailableRow(profile = profile, onAdd = { viewModel.add(profile.id) })
                    }
                }
            }
        }
    }
}

/**
 * Hand-rolled long-press-drag reorder: [localItems] mirrors [items] but is only reset from it
 * between drags (see the `remember(items)` key), so a single continuous drag gesture doesn't
 * fight incoming recompositions from [items] itself changing identity. Only commits via
 * [onReordered] on drop - not per pixel of drag - to avoid spamming the repository/DataStore.
 */
@Composable
private fun SelectedList(items: List<Profile>, onReordered: (List<String>) -> Unit, onRemove: (String) -> Unit) {
    var localItems by remember(items) { mutableStateOf(items) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        localItems.forEach { profile ->
            val isDragging = profile.id == draggingId
            NfcEmuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                    .alpha(if (isDragging) 0.85f else 1f)
                    .onGloballyPositioned { rowHeightPx = it.size.height },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.sm + Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeIconBadge(profile.fields.typeGlyph(), size = 32.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(profile.id) }) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_nocturne_close),
                            contentDescription = stringResource(R.string.manage_quick_select_remove),
                        )
                    }
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_nocturne_drag_handle),
                        contentDescription = stringResource(R.string.manage_quick_select_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(profile.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingId = profile.id; dragOffsetY = 0f },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffsetY = 0f
                                        onReordered(localItems.map { it.id })
                                    },
                                    onDragCancel = { draggingId = null; dragOffsetY = 0f },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragOffsetY += delta.y
                                        val rowHeight = rowHeightPx.toFloat()
                                        if (rowHeight <= 0f) return@detectDragGesturesAfterLongPress
                                        val fromIndex = localItems.indexOfFirst { it.id == profile.id }
                                        val steps = (dragOffsetY / rowHeight).roundToInt()
                                        val toIndex = (fromIndex + steps).coerceIn(0, localItems.lastIndex)
                                        if (toIndex != fromIndex) {
                                            localItems = localItems.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                                            dragOffsetY -= (toIndex - fromIndex) * rowHeight
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableRow(profile: Profile, onAdd: () -> Unit) {
    NfcEmuCard(modifier = Modifier.fillMaxWidth(), onClick = onAdd) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm + Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeIconBadge(profile.fields.typeGlyph(), size = 32.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(profile.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onAdd) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_nocturne_add),
                    contentDescription = stringResource(R.string.manage_quick_select_add),
                )
            }
        }
    }
}
