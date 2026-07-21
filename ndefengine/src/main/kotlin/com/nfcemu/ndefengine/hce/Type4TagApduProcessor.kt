package com.nfcemu.ndefengine.hce

import com.nfcemu.ndefengine.CapabilityContainer

/**
 * Pure-Kotlin, Android-free implementation of the NFC Forum Type 4 Tag command set
 * (ISO/IEC 7816-4 subset): SELECT (AID / file-by-id) and READ BINARY, including the
 * offset-based chunking a real reader performs when its Le is smaller than the file
 * (CC or NDEF) being read. Deliberately holds no Android imports so it can be unit
 * tested as plain JUnit, independent of an emulator/Robolectric.
 *
 * Thread-unsafe by design (a single NFC field talks to a single reader at a time);
 * the calling `HostApduService` wrapper is responsible for synchronizing access if it
 * ever calls [process] and [updateNdefMessage] from different threads concurrently.
 */
class Type4TagApduProcessor {

    private enum class SelectedFile { NONE, CC, NDEF }

    private var selectedFile: SelectedFile = SelectedFile.NONE
    private var ccBytes: ByteArray = CapabilityContainer.build(0)
    private var ndefFileBytes: ByteArray = buildNdefFile(ByteArray(0))

    /** Swaps in a new NDEF message (e.g. on profile switch) and resets file selection. */
    @Synchronized
    fun updateNdefMessage(newNdefBytes: ByteArray) {
        ccBytes = CapabilityContainer.build(newNdefBytes.size)
        ndefFileBytes = buildNdefFile(newNdefBytes)
        selectedFile = SelectedFile.NONE
    }

    /**
     * Processes one command APDU and returns the full response APDU (data + 2-byte
     * status word). Never throws - any unexpected condition (malformed APDU, bad
     * offset, unsupported instruction) results in a well-defined error status word.
     */
    @Synchronized
    fun process(commandApdu: ByteArray?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) return StatusWord.WRONG_LENGTH
        return try {
            val cla = commandApdu[0].toInt() and 0xFF
            val ins = commandApdu[1].toInt() and 0xFF
            val p1 = commandApdu[2].toInt() and 0xFF
            val p2 = commandApdu[3].toInt() and 0xFF

            if (cla != 0x00) return StatusWord.UNKNOWN_FALLBACK

            when (ins) {
                INS_SELECT -> handleSelect(commandApdu, p1, p2)
                INS_READ_BINARY -> handleReadBinary(commandApdu, p1, p2)
                else -> StatusWord.UNKNOWN_FALLBACK
            }
        } catch (e: Exception) {
            // Defensive catch-all: a malformed/adversarial APDU must never crash the service.
            StatusWord.UNKNOWN_FALLBACK
        }
    }

    private fun handleSelect(apdu: ByteArray, p1: Int, p2: Int): ByteArray {
        if (apdu.size < 5) return StatusWord.WRONG_LENGTH
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return StatusWord.WRONG_LENGTH
        val data = apdu.copyOfRange(5, 5 + lc)

        return when {
            p1 == 0x04 && p2 == 0x00 -> { // SELECT by name (AID / DF name)
                if (data.contentEquals(NDEF_TAG_AID)) {
                    selectedFile = SelectedFile.NONE
                    StatusWord.OK
                } else {
                    StatusWord.FILE_OR_APP_NOT_FOUND
                }
            }
            p1 == 0x00 && p2 == 0x0C -> { // SELECT by file id, no response data expected
                when {
                    data.contentEquals(CC_FILE_ID) -> {
                        selectedFile = SelectedFile.CC
                        StatusWord.OK
                    }
                    data.contentEquals(NDEF_FILE_ID) -> {
                        selectedFile = SelectedFile.NDEF
                        StatusWord.OK
                    }
                    else -> StatusWord.FILE_OR_APP_NOT_FOUND
                }
            }
            else -> StatusWord.WRONG_PARAMETERS_P1P2
        }
    }

    private fun handleReadBinary(apdu: ByteArray, p1: Int, p2: Int): ByteArray {
        if (apdu.size < 5) return StatusWord.WRONG_LENGTH
        val leByte = apdu[4].toInt() and 0xFF
        val le = if (leByte == 0) 256 else leByte // ISO 7816-4: Le=0x00 means "max 256 bytes"

        val file = when (selectedFile) {
            SelectedFile.CC -> ccBytes
            SelectedFile.NDEF -> ndefFileBytes
            SelectedFile.NONE -> return StatusWord.FILE_OR_APP_NOT_FOUND
        }

        // Type 4 Tags don't support short EF identifiers (P1 bit 8 = 1); reject that mode
        // explicitly rather than silently reinterpreting it as a plain offset.
        if (p1 and 0x80 != 0) return StatusWord.WRONG_PARAMETERS_P1P2
        val offset = (p1 shl 8) or p2
        if (offset > file.size) return StatusWord.WRONG_PARAMETERS_P1P2

        val available = file.size - offset
        val length = minOf(le, available)
        val slice = if (length > 0) file.copyOfRange(offset, offset + length) else ByteArray(0)
        return slice + StatusWord.OK
    }

    private fun buildNdefFile(ndefMessage: ByteArray): ByteArray {
        val nlen = ndefMessage.size
        val header = byteArrayOf((nlen shr 8).toByte(), (nlen and 0xFF).toByte())
        return header + ndefMessage
    }

    companion object {
        private const val INS_SELECT = 0xA4
        private const val INS_READ_BINARY = 0xB0

        /** NDEF Tag Application AID, D2760000850101 (NFC Forum Type 4 Tag). */
        val NDEF_TAG_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
        )
        val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03)
        val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04)
    }
}
