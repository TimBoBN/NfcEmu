package com.nfcemu.ui.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.ui.components.NfcEmuPrimaryButton
import com.nfcemu.ui.components.TypeIconBadge
import com.nfcemu.ui.components.previewText
import com.nfcemu.ui.components.typeDisplayLabel
import com.nfcemu.ui.components.typeGlyph
import com.nfcemu.ui.theme.Spacing

/**
 * Shown when content arrives via the Android share sheet (a link or phone number shared from
 * another app). Lets the user confirm/correct the detected value before broadcasting - unlike
 * [com.nfcemu.ui.profileform.ProfileFormScreen], confirming here never creates a profile; see
 * [SharePreviewViewModel.send] and [com.nfcemu.data.ProfileRepository.activateShared].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewScreen(
    onBack: () -> Unit,
    onActivated: () -> Unit,
    viewModel: SharePreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val payload = SharedContentDetector.toPayload(uiState.text, uiState.type)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_nocturne_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeIconBadge(payload.typeGlyph(), size = 32.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(payload.typeDisplayLabel(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                label = { Text(stringResource(R.string.share_preview_content_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                payload.previewText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))
            NfcEmuPrimaryButton(
                onClick = { viewModel.send(onActivated) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.share_preview_send))
            }
        }
    }
}
