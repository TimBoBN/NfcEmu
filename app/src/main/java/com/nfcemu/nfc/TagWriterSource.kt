package com.nfcemu.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Result of attempting to write an NDEF message onto a physically-tapped tag. */
sealed interface TagWriteResult {
    data object Success : TagWriteResult
    data class Failure(val reason: String) : TagWriteResult
}

/**
 * Narrow, mockable contract around [NfcAdapter.enableReaderMode]/[disableReaderMode] for
 * writing - the mirror of [TagReaderSource], kept as a **separate** interface (not merged
 * into it) since each has exactly one concern, matching this codebase's other
 * Android-framework seams. Scoping and lifecycle rules are identical to [TagReaderSource] -
 * see that interface's kdoc.
 */
interface TagWriterSource {
    fun startWriting(activity: Activity, ndefBytes: ByteArray, onResult: (TagWriteResult) -> Unit)
    fun stopWriting(activity: Activity)
}

@Singleton
class NfcAdapterTagWriterSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : TagWriterSource {

    override fun startWriting(activity: Activity, ndefBytes: ByteArray, onResult: (TagWriteResult) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return
        adapter.enableReaderMode(
            activity,
            { tag -> onResult(writeNdef(tag, ndefBytes)) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
            null,
        )
    }

    override fun stopWriting(activity: Activity) {
        NfcAdapter.getDefaultAdapter(context)?.disableReaderMode(activity)
    }

    /** Runs on a binder thread, per [NfcAdapter.ReaderCallback] - never touches UI directly. */
    private fun writeNdef(tag: Tag, ndefBytes: ByteArray): TagWriteResult {
        val ndef = Ndef.get(tag)
        return if (ndef != null) {
            writeToFormattedTag(ndef, ndefBytes)
        } else {
            writeToBlankTag(tag, ndefBytes)
        }
    }

    private fun writeToFormattedTag(ndef: Ndef, ndefBytes: ByteArray): TagWriteResult {
        return try {
            ndef.connect()
            if (!ndef.isWritable) return TagWriteResult.Failure("This tag is read-only")
            if (ndef.maxSize < ndefBytes.size) {
                return TagWriteResult.Failure(
                    "This tag is too small to hold this profile (needs ${ndefBytes.size} bytes, tag holds ${ndef.maxSize})",
                )
            }
            ndef.writeNdefMessage(NdefMessage(ndefBytes))
            TagWriteResult.Success
        } catch (e: Exception) {
            TagWriteResult.Failure(e.message ?: "Could not write to this tag")
        } finally {
            runCatching { ndef.close() }
        }
    }

    private fun writeToBlankTag(tag: Tag, ndefBytes: ByteArray): TagWriteResult {
        val formatable = NdefFormatable.get(tag) ?: return TagWriteResult.Failure("This tag cannot be written to")
        return try {
            formatable.connect()
            formatable.format(NdefMessage(ndefBytes))
            TagWriteResult.Success
        } catch (e: Exception) {
            TagWriteResult.Failure(e.message ?: "Could not write to this tag")
        } finally {
            runCatching { formatable.close() }
        }
    }
}
