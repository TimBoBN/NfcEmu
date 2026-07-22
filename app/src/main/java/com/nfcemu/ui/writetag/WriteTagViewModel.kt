package com.nfcemu.ui.writetag

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nfcemu.data.ProfileRepository
import com.nfcemu.ndefengine.AarConfig
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.nfc.TagWriteResult
import com.nfcemu.nfc.TagWriterSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface WriteTagUiState {
    data object Waiting : WriteTagUiState
    data object ProfileNotFound : WriteTagUiState
    data object Success : WriteTagUiState
    data class Failure(val reason: String) : WriteTagUiState
}

/**
 * [startWriting]/[stopWriting] just forward an [Activity] through to [TagWriterSource] -
 * they don't retain it on the ViewModel, so this doesn't leak - same pattern as
 * [com.nfcemu.ui.scantag.ScanTagViewModel].
 */
@HiltViewModel
class WriteTagViewModel @Inject constructor(
    private val tagWriterSource: TagWriterSource,
    profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: String? = savedStateHandle["profileId"]
    private val profile = profileId?.let { id -> profileRepository.profiles.value.find { it.id == id } }

    val profileName: String = profile?.name.orEmpty()

    // Encoded once, at construction, and reused for every tag tapped during this screen's lifetime.
    private val ndefBytes: ByteArray? = profile?.let {
        NdefMessageFactory.build(it.fields, it.aarPackageName?.let(::AarConfig))
    }

    private val _uiState = MutableStateFlow(if (profile == null) WriteTagUiState.ProfileNotFound else WriteTagUiState.Waiting)
    val uiState: StateFlow<WriteTagUiState> = _uiState.asStateFlow()

    fun startWriting(activity: Activity) {
        val bytes = ndefBytes ?: return
        tagWriterSource.startWriting(activity, bytes, ::onWriteResult)
    }

    fun stopWriting(activity: Activity) {
        tagWriterSource.stopWriting(activity)
    }

    /** Resets from [WriteTagUiState.Failure] or [WriteTagUiState.Success] back to [WriteTagUiState.Waiting]. */
    fun dismissResult() {
        if (profile != null) _uiState.value = WriteTagUiState.Waiting
    }

    /**
     * Runs on a binder thread (see [TagWriterSource]). Internal (not private) so tests can
     * drive it directly without needing a real [Activity], which plain JUnit (no Robolectric)
     * can't construct.
     */
    internal fun onWriteResult(result: TagWriteResult) {
        _uiState.value = when (result) {
            is TagWriteResult.Success -> WriteTagUiState.Success
            is TagWriteResult.Failure -> WriteTagUiState.Failure(result.reason)
        }
    }
}
