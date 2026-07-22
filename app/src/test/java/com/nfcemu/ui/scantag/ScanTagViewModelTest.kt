package com.nfcemu.ui.scantag

import android.app.Activity
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.nfc.TagReadResult
import com.nfcemu.nfc.TagReaderSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeTagReaderSource : TagReaderSource {
    override fun startReading(activity: Activity, onTagRead: (TagReadResult) -> Unit) = Unit
    override fun stopReading(activity: Activity) = Unit
}

/**
 * Exercises [ScanTagViewModel.onTagRead] directly (it's `internal` for exactly this reason -
 * see its kdoc) rather than through [ScanTagViewModel.startScanning], since a plain JUnit
 * test (no Robolectric) can't construct a real [Activity] to pass through to [TagReaderSource].
 */
class ScanTagViewModelTest {

    @Test
    fun `starts in the waiting state`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())
        assertIs<ScanTagUiState.Waiting>(viewModel.uiState.value)
    }

    @Test
    fun `a successfully read and decoded tag surfaces as scanned`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())
        val bytes = NdefMessageFactory.build(NdefPayload.Text("hello"))

        viewModel.onTagRead(TagReadResult.Success(bytes))

        val state = assertIs<ScanTagUiState.Scanned>(viewModel.uiState.value)
        assertEquals(NdefPayload.Text("hello"), state.payload)
    }

    @Test
    fun `a tag carrying an aar record surfaces its package name too`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())
        val bytes = NdefMessageFactory.build(NdefPayload.Text("hello"), com.nfcemu.ndefengine.AarConfig("com.example.app"))

        viewModel.onTagRead(TagReadResult.Success(bytes))

        val state = assertIs<ScanTagUiState.Scanned>(viewModel.uiState.value)
        assertEquals("com.example.app", state.aarPackageName)
    }

    @Test
    fun `a read failure surfaces as unsupported with the failure reason`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())

        viewModel.onTagRead(TagReadResult.Failure("no NDEF tech on this tag"))

        val state = assertIs<ScanTagUiState.Unsupported>(viewModel.uiState.value)
        assertEquals("no NDEF tech on this tag", state.reason)
    }

    @Test
    fun `an empty tag surfaces as unsupported rather than a blank payload`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())

        viewModel.onTagRead(TagReadResult.Success(ByteArray(0)))

        assertIs<ScanTagUiState.Unsupported>(viewModel.uiState.value)
    }

    @Test
    fun `dismissResult returns to waiting`() {
        val viewModel = ScanTagViewModel(FakeTagReaderSource())
        viewModel.onTagRead(TagReadResult.Failure("x"))

        viewModel.dismissResult()

        assertIs<ScanTagUiState.Waiting>(viewModel.uiState.value)
    }
}
