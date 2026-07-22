package com.nfcemu.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Result of attempting to read an NDEF message off a physically-tapped tag. */
sealed interface TagReadResult {
    data class Success(val ndefBytes: ByteArray) : TagReadResult
    data class Failure(val reason: String) : TagReadResult
}

/**
 * Narrow, mockable contract around [NfcAdapter.enableReaderMode]/[disableReaderMode], so
 * [com.nfcemu.ui.scantag.ScanTagViewModel] doesn't depend on the concrete Android API
 * directly - same pattern as [NfcStateSource]/[NfcStateObserver].
 *
 * This is deliberately *not* the same mechanism as [com.nfcemu.ui.MainActivity]'s
 * foreground-dispatch tag-discovery suppression: reader mode is a stronger, additive
 * override that Android applies on top of that, scoped to exactly the lifetime of the
 * screen that calls [startReading] - `MainActivity`'s suppression needs no changes at
 * all and doesn't need to know scanning ever happened.
 */
interface TagReaderSource {
    fun startReading(activity: Activity, onTagRead: (TagReadResult) -> Unit)
    fun stopReading(activity: Activity)
}

@Singleton
class NfcAdapterTagReaderSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : TagReaderSource {

    override fun startReading(activity: Activity, onTagRead: (TagReadResult) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return
        adapter.enableReaderMode(
            activity,
            { tag -> onTagRead(readNdef(tag)) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    override fun stopReading(activity: Activity) {
        NfcAdapter.getDefaultAdapter(context)?.disableReaderMode(activity)
    }

    /** Runs on a binder thread, per [NfcAdapter.ReaderCallback] - never touches UI directly. */
    private fun readNdef(tag: Tag): TagReadResult {
        val ndef = Ndef.get(tag) ?: return TagReadResult.Failure("This tag has no readable NDEF data")
        return try {
            ndef.connect()
            val message = ndef.ndefMessage ?: return TagReadResult.Failure("This tag is empty")
            TagReadResult.Success(message.toByteArray())
        } catch (e: Exception) {
            TagReadResult.Failure(e.message ?: "Could not read this tag")
        } finally {
            runCatching { ndef.close() }
        }
    }
}
