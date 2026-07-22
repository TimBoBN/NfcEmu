package com.nfcemu.ui.scantag

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.nfcemu.ndefengine.DecodedTagResult
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ndefengine.NdefPayloadDecoder
import com.nfcemu.nfc.TagReadResult
import com.nfcemu.nfc.TagReaderSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface ScanTagUiState {
    data object Waiting : ScanTagUiState
    data class Scanned(val payload: NdefPayload, val aarPackageName: String?) : ScanTagUiState
    data class Unsupported(val reason: String) : ScanTagUiState
}

/**
 * [startScanning]/[stopScanning] just forward an [Activity] through to [TagReaderSource] -
 * they don't retain it on the ViewModel, so this doesn't leak. The screen is expected to
 * call [startScanning] from a `DisposableEffect` keyed to its own lifetime and [stopScanning]
 * from that effect's cleanup, so reader mode is never active longer than this screen is on
 * screen (see [TagReaderSource]'s kdoc for why that matters).
 */
@HiltViewModel
class ScanTagViewModel @Inject constructor(
    private val tagReaderSource: TagReaderSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanTagUiState>(ScanTagUiState.Waiting)
    val uiState: StateFlow<ScanTagUiState> = _uiState.asStateFlow()

    fun startScanning(activity: Activity) {
        tagReaderSource.startReading(activity, ::onTagRead)
    }

    fun stopScanning(activity: Activity) {
        tagReaderSource.stopReading(activity)
    }

    fun dismissResult() {
        _uiState.value = ScanTagUiState.Waiting
    }

    /**
     * Runs on a binder thread (see [TagReaderSource]) - [MutableStateFlow.update] is
     * thread-safe. Internal (not private) so tests can drive it directly without needing a
     * real [Activity] instance, which plain JUnit (no Robolectric) can't construct.
     */
    internal fun onTagRead(result: TagReadResult) {
        _uiState.update {
            when (result) {
                is TagReadResult.Failure -> ScanTagUiState.Unsupported(result.reason)
                is TagReadResult.Success -> when (val decoded = NdefPayloadDecoder.decode(result.ndefBytes)) {
                    is DecodedTagResult.Success -> ScanTagUiState.Scanned(decoded.payload, decoded.aarPackageName)
                    is DecodedTagResult.Unsupported -> ScanTagUiState.Unsupported(decoded.reason)
                    DecodedTagResult.Empty -> ScanTagUiState.Unsupported("This tag is empty")
                }
            }
        }
    }
}
