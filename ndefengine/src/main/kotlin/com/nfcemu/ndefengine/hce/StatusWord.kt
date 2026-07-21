package com.nfcemu.ndefengine.hce

/**
 * ISO/IEC 7816-4 status words used by [Type4TagApduProcessor]. Deliberately kept to a
 * small, well-defined set so every response is unambiguous: success, file/AID not
 * found, bad P1-P2 (e.g. out-of-range offset), wrong length, and a catch-all fallback
 * for anything not explicitly handled (unknown instruction, unsupported class, ...).
 */
internal object StatusWord {
    val OK = byteArrayOf(0x90.toByte(), 0x00)
    val FILE_OR_APP_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val WRONG_PARAMETERS_P1P2 = byteArrayOf(0x6A.toByte(), 0x86.toByte())
    val WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00)
    val UNKNOWN_FALLBACK = byteArrayOf(0x6F.toByte(), 0x00)
}
