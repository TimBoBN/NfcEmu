package com.nfcemu.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.data.library.LibraryEntry
import com.nfcemu.data.library.LibraryEntryDirection
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var entryPendingDelete by remember { mutableStateOf<LibraryEntry?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFile)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.library_import))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                items(entries, key = { it.id }) { entry ->
                    LibraryRow(
                        entry = entry,
                        onLoadActive = { viewModel.loadAsActive(entry) },
                        onShare = { shareEntry(context, entry) },
                        onDelete = { entryPendingDelete = entry },
                    )
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }

    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = { Text(stringResource(R.string.library_delete_message, entry.displayName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEntry(entry.id); entryPendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun shareEntry(context: android.content.Context, entry: LibraryEntry) {
    val uri = Uri.parse(entry.uri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, entry.displayName))
}

@Composable
private fun LibraryRow(
    entry: LibraryEntry,
    onLoadActive: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.displayName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = if (entry.direction == LibraryEntryDirection.EXPORTED) {
                            stringResource(R.string.library_exported)
                        } else {
                            stringResource(R.string.library_imported)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = DateFormat.getDateTimeInstance().format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            androidx.compose.material3.Button(onClick = onLoadActive) {
                Text(stringResource(R.string.library_load_active))
            }
        }
    }
}
