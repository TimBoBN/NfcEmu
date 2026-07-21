package com.nfcemu.ndefengine

/**
 * NFC Forum Type 4 Tag Capability Container (file id E103), see [NFC Forum Type 4 Tag
 * Operation Specification 3.2](https://nfc-forum.org) section 4.3. Fixed layout with a
 * single NDEF File Control TLV; only the "Max NDEF file size" field varies with the
 * actual message length, which is why this is generated dynamically rather than
 * hard-coded.
 */
object CapabilityContainer {

    /** Max R-APDU/C-APDU data size we advertise; matches the chunk size used by the HCE service. */
    const val MAX_LE = 0xF6
    const val MAX_LC = 0xF6

    private const val CC_LENGTH = 15

    fun build(ndefMessageLength: Int): ByteArray {
        require(ndefMessageLength in 0..0xFFFD) { "NDEF message too large for a Type 4 Tag (max 65533 bytes)" }
        val maxNdefFileSize = ndefMessageLength + 2 // + 2-byte NLEN length prefix in the NDEF file

        return byteArrayOf(
            (CC_LENGTH shr 8).toByte(), (CC_LENGTH and 0xFF).toByte(), // CCLEN
            0x20, // Mapping version 2.0
            (MAX_LE shr 8).toByte(), (MAX_LE and 0xFF).toByte(), // MLe
            (MAX_LC shr 8).toByte(), (MAX_LC and 0xFF).toByte(), // MLc
            0x04, 0x06, // NDEF File Control TLV: T=04, L=06
            0xE1.toByte(), 0x04, // File Identifier E104
            (maxNdefFileSize shr 8).toByte(), (maxNdefFileSize and 0xFF).toByte(), // Max NDEF file size
            0x00, // Read access: granted
            0xFF.toByte(), // Write access: denied (read-only tag)
        )
    }
}
