package com.nfcemu.ndefengine

/** NFC Forum TNF (Type Name Format) values, see NDEF binary spec section 3.2.6. */
object Tnf {
    const val EMPTY = 0x00
    const val WELL_KNOWN = 0x01
    const val MIME_MEDIA = 0x02
    const val ABSOLUTE_URI = 0x03
    const val EXTERNAL_TYPE = 0x04
    const val UNKNOWN = 0x05
    const val UNCHANGED = 0x06
}

/**
 * A single NDEF record prior to MB/ME/CF/SR/IL flag assignment, which only becomes
 * known once the record's position within the final message is decided by
 * [NdefMessageEncoder.encode].
 */
data class RawNdefRecord(
    val tnf: Int,
    val type: ByteArray,
    val id: ByteArray = ByteArray(0),
    val payload: ByteArray,
)
