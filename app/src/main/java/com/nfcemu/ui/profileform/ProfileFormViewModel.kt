package com.nfcemu.ui.profileform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.ndefengine.AarConfig
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ui.components.previewText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileFormUiState(
    val template: ProfileTypeTemplate = ProfileTypeTemplate.TEXT,
    val isEditing: Boolean = false,
    val name: String = "",
    val fields: ProfileFormFields = ProfileFormFields.Text(),
    val aarEnabled: Boolean = false,
    val aarPackageName: String = "",
    val validation: FormValidationResult = FormValidationResult(emptyMap()),
    val nameError: String? = null,
    val aarError: String? = null,
    val estimatedNdefSize: Int = 0,
    val previewText: String = "",
    val saved: Boolean = false,
) {
    val isValid: Boolean
        get() = validation.isValid && nameError == null && aarError == null
}

@HiltViewModel
class ProfileFormViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingProfileId: String? = savedStateHandle["profileId"]
    private val existingProfile: Profile? = editingProfileId?.let { id ->
        profileRepository.profiles.value.find { it.id == id }
    }

    private val _uiState = MutableStateFlow(buildInitialState(savedStateHandle))
    val uiState: StateFlow<ProfileFormUiState> = _uiState.asStateFlow()

    init {
        recompute()
    }

    private fun buildInitialState(savedStateHandle: SavedStateHandle): ProfileFormUiState {
        val profile = existingProfile
        return if (profile != null) {
            val fields = ProfileFormCodec.toFormFields(profile.fields)
            ProfileFormUiState(
                template = fields.template,
                isEditing = true,
                name = profile.name,
                fields = fields,
                aarEnabled = profile.aarPackageName != null,
                aarPackageName = profile.aarPackageName.orEmpty(),
            )
        } else {
            val template = savedStateHandle.get<String>("template")
                ?.let { runCatching { ProfileTypeTemplate.valueOf(it) }.getOrNull() }
                ?: ProfileTypeTemplate.TEXT
            ProfileFormUiState(template = template, isEditing = false, fields = ProfileFormFields.initialFor(template))
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
        recompute()
    }

    fun updateFields(fields: ProfileFormFields) {
        _uiState.update { it.copy(fields = fields) }
        recompute()
    }

    fun setAarEnabled(enabled: Boolean) {
        _uiState.update { it.copy(aarEnabled = enabled) }
        recompute()
    }

    fun updateAarPackageName(packageName: String) {
        _uiState.update { it.copy(aarPackageName = packageName) }
        recompute()
    }

    private fun recompute() {
        _uiState.update { state ->
            val validation = ProfileFormCodec.validate(state.fields)
            val nameError = if (state.name.isBlank()) "Please enter a name for the profile" else null
            val aarError = if (state.aarEnabled && !isValidPackageName(state.aarPackageName)) {
                "Please enter a valid package name (e.g. com.example.app)"
            } else null

            var estimatedSize = 0
            var previewText = ""
            if (validation.isValid) {
                val payload = ProfileFormCodec.toPayload(state.fields)
                previewText = payload.previewText()
                val aar = if (state.aarEnabled && aarError == null) AarConfig(state.aarPackageName.trim()) else null
                estimatedSize = runCatching { NdefMessageFactory.build(payload, aar).size }.getOrDefault(0)
            }

            state.copy(
                validation = validation,
                nameError = nameError,
                aarError = aarError,
                estimatedNdefSize = estimatedSize,
                previewText = previewText,
            )
        }
    }

    private fun isValidPackageName(value: String): Boolean =
        value.trim().matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))

    fun save() {
        val state = _uiState.value
        if (!state.isValid) return
        val payload = ProfileFormCodec.toPayload(state.fields)
        val aarPackage = if (state.aarEnabled) state.aarPackageName.trim() else null

        viewModelScope.launch {
            if (state.isEditing && existingProfile != null) {
                profileRepository.updateProfile(
                    existingProfile.copy(name = state.name.trim(), fields = payload, aarPackageName = aarPackage),
                )
            } else {
                profileRepository.createProfile(
                    Profile(name = state.name.trim(), fields = payload, aarPackageName = aarPackage),
                )
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
