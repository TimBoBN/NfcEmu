package com.nfcemu.ui.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfcemu.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharePreviewUiState(
    val text: String = "",
    val type: SharedContentType = SharedContentType.TEXT,
) {
    val isValid: Boolean get() = text.isNotBlank()
}

/**
 * Backs the "confirm before sending" screen shown when content arrives via the Android share
 * sheet. Deliberately doesn't touch [ProfileRepository.createProfile] - [send] activates the
 * content as the ephemeral [com.nfcemu.data.Profile.SHARED_ID] row instead, so nothing is
 * saved to the profile list unless the user later chooses to on the Transmit screen.
 */
@HiltViewModel
class SharePreviewViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialText = savedStateHandle.get<String>("sharedText")
        ?.let(SharedTextCodec::decode)
        .orEmpty()

    private val _uiState = MutableStateFlow(
        SharePreviewUiState(text = initialText, type = SharedContentDetector.classify(initialText)),
    )
    val uiState: StateFlow<SharePreviewUiState> = _uiState.asStateFlow()

    fun updateText(text: String) {
        _uiState.update { SharePreviewUiState(text = text, type = SharedContentDetector.classify(text)) }
    }

    fun send(onActivated: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        val payload = SharedContentDetector.toPayload(state.text, state.type)
        viewModelScope.launch {
            profileRepository.activateShared(payload, displayName = state.text.trim())
            onActivated()
        }
    }
}
