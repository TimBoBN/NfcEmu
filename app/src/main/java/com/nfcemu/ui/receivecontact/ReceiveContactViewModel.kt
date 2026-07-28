package com.nfcemu.ui.receivecontact

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

sealed interface ReceiveContactUiState {
    data object Waiting : ReceiveContactUiState
    data class Scanned(val vcard: NdefPayload.VCard) : ReceiveContactUiState
    data class Unsupported(val reason: String) : ReceiveContactUiState
}

/**
 * Near-identical to [com.nfcemu.ui.scantag.ScanTagViewModel] (same [TagReaderSource], same
 * start/stop-scanning lifecycle contract) but scoped to vCard content only - any other
 * payload type becomes [ReceiveContactUiState.Unsupported], since this screen only makes
 * sense for actual contact cards. Doesn't persist anything itself: [ReceiveContactUiState.Scanned]
 * carries the raw [NdefPayload.VCard] straight to [com.nfcemu.ui.receivecontact.ReceiveContactScreen],
 * which hands it to the system Contacts app via an `ACTION_INSERT` intent - keeping Android's
 * `Context`/`Intent` framework types out of this ViewModel, same as every other Android-boundary
 * seam in this app.
 */
@HiltViewModel
class ReceiveContactViewModel @Inject constructor(
    private val tagReaderSource: TagReaderSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiveContactUiState>(ReceiveContactUiState.Waiting)
    val uiState: StateFlow<ReceiveContactUiState> = _uiState.asStateFlow()

    fun startScanning(activity: Activity) {
        tagReaderSource.startReading(activity, ::onTagRead)
    }

    fun stopScanning(activity: Activity) {
        tagReaderSource.stopReading(activity)
    }

    fun dismissResult() {
        _uiState.value = ReceiveContactUiState.Waiting
    }

    /** Runs on a binder thread, per [TagReaderSource] - see ScanTagViewModel's kdoc. */
    internal fun onTagRead(result: TagReadResult) {
        _uiState.update {
            when (result) {
                is TagReadResult.Failure -> ReceiveContactUiState.Unsupported(result.reason)
                is TagReadResult.Success -> when (val decoded = NdefPayloadDecoder.decode(result.ndefBytes)) {
                    is DecodedTagResult.Success -> {
                        val payload = decoded.payload
                        if (payload is NdefPayload.VCard) {
                            ReceiveContactUiState.Scanned(payload)
                        } else {
                            ReceiveContactUiState.Unsupported("This tag isn't a contact card")
                        }
                    }
                    is DecodedTagResult.Unsupported -> ReceiveContactUiState.Unsupported(decoded.reason)
                    DecodedTagResult.Empty -> ReceiveContactUiState.Unsupported("This tag is empty")
                }
            }
        }
    }
}
