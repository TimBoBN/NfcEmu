package com.nfcemu.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
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
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE,
            null,
        )
    }

    override fun stopReading(activity: Activity) {
        NfcAdapter.getDefaultAdapter(context)?.disableReaderMode(activity)
    }

    /**
     * Runs on a binder thread, per [NfcAdapter.ReaderCallback] - never touches UI directly.
     * [TagLostException] is the common real-world failure mode (a slightly unsteady hand, or a
     * marginal field, frequent with NTAG21x-style Type 2 tags, drops the connection mid-read),
     * so it gets a couple of quick reconnect attempts before surfacing as an error - other
     * exceptions (format errors, I/O errors) aren't transient the same way and fail immediately.
     */
    private fun readNdef(tag: Tag): TagReadResult {
        val ndef = Ndef.get(tag) ?: return nonNdefFailure(tag)
        repeat(MAX_READ_ATTEMPTS) { attempt ->
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: return TagReadResult.Failure("This tag is empty")
                return TagReadResult.Success(message.toByteArray())
            } catch (e: TagLostException) {
                if (attempt == MAX_READ_ATTEMPTS - 1) {
                    return TagReadResult.Failure("Tag moved away before it could be read - hold it steady against the back of your phone")
                }
                Thread.sleep(RETRY_DELAY_MS)
            } catch (e: Exception) {
                return TagReadResult.Failure(e.message ?: "Could not read this tag")
            } finally {
                runCatching { ndef.close() }
            }
        }
        error("unreachable: every branch of the repeat block above returns")
    }

    /** No NDEF technology at all (raw UID tag, unformatted MIFARE Classic, ...) - name what was found instead of a generic message. */
    private fun nonNdefFailure(tag: Tag): TagReadResult {
        val uid = tag.id?.joinToString("") { "%02X".format(it) }.orEmpty()
        val techName = tag.techList
            .map { it.substringAfterLast('.') }
            .firstOrNull { it != "NdefFormatable" }
        val description = techName?.let { "$it tag" } ?: "This tag"
        val uidSuffix = if (uid.isNotEmpty()) " (UID $uid)" else ""
        return TagReadResult.Failure("$description$uidSuffix has no readable NDEF data")
    }

    private companion object {
        const val MAX_READ_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 120L
    }
}
