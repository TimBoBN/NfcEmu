package com.nfcemu.ui.scantag

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanTagScreen(
    onBack: () -> Unit,
    onScanned: (ScannedTag) -> Unit,
    viewModel: ScanTagViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity != null) viewModel.startScanning(activity)
        onDispose {
            if (activity != null) viewModel.stopScanning(activity)
        }
    }

    LaunchedEffect(uiState) {
        val scanned = uiState as? ScanTagUiState.Scanned ?: return@LaunchedEffect
        onScanned(ScannedTag(scanned.payload, scanned.aarPackageName))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_tag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                is ScanTagUiState.Unsupported -> {
                    Icon(
                        imageVector = Icons.Filled.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer()
                    Text(stringResource(R.string.scan_tag_error_title), style = MaterialTheme.typography.titleMedium)
                    Spacer()
                    Text(
                        state.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer()
                    Button(onClick = viewModel::dismissResult) {
                        Text(stringResource(R.string.scan_tag_try_again))
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer()
                    Text(
                        stringResource(R.string.scan_tag_waiting),
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

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
