package com.nfcemu.ui.profileform

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nfcemu.R
import com.nfcemu.ndefengine.WifiAuthType
import com.nfcemu.ui.theme.Motion
import com.nfcemu.ui.theme.Spacing
import com.nfcemu.util.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) stringResource(R.string.profile_form_edit_title) else stringResource(R.string.profile_form_new_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState())
                .animateContentSize(Motion.standard()),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.profile_form_name)) },
                isError = uiState.nameError != null,
                supportingText = { uiState.nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer()

            TemplateFormBody(fields = uiState.fields, errors = uiState.validation.errors, onChange = viewModel::updateFields)

            Spacer()
            AarSection(
                enabled = uiState.aarEnabled,
                packageName = uiState.aarPackageName,
                error = uiState.aarError,
                installedApps = installedApps,
                onEnabledChange = viewModel::setAarEnabled,
                onPackageNameChange = viewModel::updateAarPackageName,
            )

            Spacer()
            PreviewCard(previewText = uiState.previewText, estimatedSize = uiState.estimatedNdefSize, isValid = uiState.isValid)

            Spacer()
            Button(
                onClick = viewModel::save,
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun Spacer() = androidx.compose.foundation.layout.Spacer(Modifier.padding(top = Spacing.sm + Spacing.xs))

@Composable
private fun TemplateFormBody(
    fields: ProfileFormFields,
    errors: Map<String, String>,
    onChange: (ProfileFormFields) -> Unit,
) {
    when (fields) {
        is ProfileFormFields.Website -> LabeledField(
            value = fields.url, label = stringResource(R.string.field_website_url), error = errors["url"],
            onChange = { onChange(fields.copy(url = it)) }, keyboardType = KeyboardType.Uri,
        )
        is ProfileFormFields.Phone -> LabeledField(
            value = fields.number, label = stringResource(R.string.field_phone_number), error = errors["number"],
            onChange = { onChange(fields.copy(number = it)) }, keyboardType = KeyboardType.Phone,
        )
        is ProfileFormFields.Email -> Column {
            LabeledField(fields.address, stringResource(R.string.field_email_address), errors["address"], { onChange(fields.copy(address = it)) }, KeyboardType.Email)
            Spacer()
            LabeledField(fields.subject, stringResource(R.string.field_email_subject), null, { onChange(fields.copy(subject = it)) })
            Spacer()
            LabeledField(fields.body, stringResource(R.string.field_email_body), null, { onChange(fields.copy(body = it)) })
        }
        is ProfileFormFields.Sms -> Column {
            LabeledField(fields.number, stringResource(R.string.field_phone_number), errors["number"], { onChange(fields.copy(number = it)) }, KeyboardType.Phone)
            Spacer()
            LabeledField(fields.body, stringResource(R.string.field_sms_body), null, { onChange(fields.copy(body = it)) })
        }
        is ProfileFormFields.Location -> Column {
            LabeledField(fields.latitude, stringResource(R.string.field_latitude), errors["latitude"], { onChange(fields.copy(latitude = it)) }, KeyboardType.Decimal)
            Spacer()
            LabeledField(fields.longitude, stringResource(R.string.field_longitude), errors["longitude"], { onChange(fields.copy(longitude = it)) }, KeyboardType.Decimal)
        }
        is ProfileFormFields.PlayStore -> LabeledField(
            fields.appId, stringResource(R.string.field_play_store_id), errors["appId"], { onChange(fields.copy(appId = it)) },
        )
        is ProfileFormFields.Wifi -> WifiForm(fields, errors, onChange)
        is ProfileFormFields.VCard -> VCardForm(fields, onChange)
        is ProfileFormFields.Text -> Column {
            LabeledField(fields.text, stringResource(R.string.field_text), errors["text"], { onChange(fields.copy(text = it)) })
            Spacer()
            LabeledField(fields.languageCode, stringResource(R.string.field_language_code), null, { onChange(fields.copy(languageCode = it)) })
        }
        is ProfileFormFields.CustomUri -> LabeledField(
            fields.uri, stringResource(R.string.field_custom_uri), errors["uri"], { onChange(fields.copy(uri = it)) }, KeyboardType.Uri,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiForm(fields: ProfileFormFields.Wifi, errors: Map<String, String>, onChange: (ProfileFormFields) -> Unit) {
    Column {
        LabeledField(fields.ssid, stringResource(R.string.field_wifi_ssid), errors["ssid"], { onChange(fields.copy(ssid = it)) })
        Spacer()
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = fields.authType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_wifi_auth_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                WifiAuthType.entries.forEach { authType ->
                    DropdownMenuItem(
                        text = { Text(authType.name) },
                        onClick = { onChange(fields.copy(authType = authType)); expanded = false },
                    )
                }
            }
        }
        Spacer()
        LabeledField(
            fields.password, stringResource(R.string.field_wifi_password), errors["password"],
            { onChange(fields.copy(password = it)) }, isPassword = true,
        )
    }
}

@Composable
private fun VCardForm(fields: ProfileFormFields.VCard, onChange: (ProfileFormFields) -> Unit) {
    Column {
        LabeledField(fields.name, stringResource(R.string.field_vcard_name), null, { onChange(fields.copy(name = it)) })
        Spacer()
        fields.phones.forEachIndexed { index, phone ->
            LabeledField(phone, stringResource(R.string.field_vcard_phone), null, { value ->
                onChange(fields.copy(phones = fields.phones.toMutableList().apply { set(index, value) }))
            }, KeyboardType.Phone)
            Spacer()
        }
        TextButton(onClick = { onChange(fields.copy(phones = fields.phones + "")) }) {
            Text(stringResource(R.string.action_add_phone))
        }
        Spacer()
        fields.emails.forEachIndexed { index, email ->
            LabeledField(email, stringResource(R.string.field_vcard_email), null, { value ->
                onChange(fields.copy(emails = fields.emails.toMutableList().apply { set(index, value) }))
            }, KeyboardType.Email)
            Spacer()
        }
        TextButton(onClick = { onChange(fields.copy(emails = fields.emails + "")) }) {
            Text(stringResource(R.string.action_add_email))
        }
        Spacer()
        LabeledField(fields.organization, stringResource(R.string.field_vcard_organization), null, { onChange(fields.copy(organization = it)) })
        Spacer()
        LabeledField(fields.title, stringResource(R.string.field_vcard_title), null, { onChange(fields.copy(title = it)) })
        Spacer()
        LabeledField(fields.website, stringResource(R.string.field_vcard_website), null, { onChange(fields.copy(website = it)) }, KeyboardType.Uri)
        Spacer()
        LabeledField(fields.address, stringResource(R.string.field_vcard_address), null, { onChange(fields.copy(address = it)) })
    }
}

@Composable
private fun LabeledField(
    value: String,
    label: String,
    error: String?,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun AarSection(
    enabled: Boolean,
    packageName: String,
    error: String?,
    installedApps: List<InstalledApp>,
    onEnabledChange: (Boolean) -> Unit,
    onPackageNameChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
            Text(stringResource(R.string.field_aar_checkbox))
        }
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(Motion.standard()) + fadeIn(Motion.standard()),
            exit = shrinkVertically(Motion.standard()) + fadeOut(Motion.standard()),
        ) {
            Column {
                Spacer()
                OutlinedTextField(
                    value = packageName,
                    onValueChange = onPackageNameChange,
                    label = { Text(stringResource(R.string.field_aar_package)) },
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    trailingIcon = {
                        IconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Filled.Apps, contentDescription = stringResource(R.string.field_aar_pick_app))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            apps = installedApps,
            onDismiss = { showPicker = false },
            onSelect = { app ->
                onPackageNameChange(app.packageName)
                showPicker = false
            },
        )
    }
}

@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSelect: (InstalledApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.field_aar_pick_app_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.action_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer()
                if (apps.isEmpty()) {
                    Text(
                        stringResource(R.string.field_aar_pick_app_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.field_aar_pick_app_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(app) }
                                    .padding(vertical = Spacing.sm),
                            ) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PreviewCard(previewText: String, estimatedSize: Int, isValid: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md).animateContentSize(Motion.standard())) {
            Text(stringResource(R.string.profile_form_preview_title), style = MaterialTheme.typography.labelLarge)
            Spacer()
            AnimatedContent(
                targetState = if (isValid) previewText else null,
                transitionSpec = { fadeIn(Motion.standard()) togetherWith fadeOut(tween(Motion.DURATION_SHORT)) },
                label = "form-preview-text",
            ) { text ->
                Text(
                    text ?: stringResource(R.string.profile_form_preview_incomplete),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (isValid) {
                Spacer()
                Text(
                    stringResource(R.string.profile_form_estimated_size, estimatedSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
