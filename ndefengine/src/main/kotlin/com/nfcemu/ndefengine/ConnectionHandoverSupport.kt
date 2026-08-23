package com.nfcemu.ndefengine

/**
 * Shared "Handover Select" envelope for the single-carrier connection-handover payloads
 * ([WifiHandoverRecordEncoder], [BluetoothHandoverRecordEncoder]): a Well-Known "Hs" record
 * wrapping one Alternative Carrier ("ac") record, followed by the carrier's own MIME record.
 * See NFC Forum Connection Handover 1.5, section 6.
 */
internal object ConnectionHandoverSupport {

    /** [carrierRecord] must reference [carrierRef] via its own `id` field. */
    fun buildHandoverMessage(carrierRecord: RawNdefRecord, carrierRef: String): List<RawNdefRecord> {
        val acRecord = RawNdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = byteArrayOf('a'.code.toByte(), 'c'.code.toByte()),
            payload = buildAcPayload(carrierRef),
        )
        val embeddedMessage = NdefMessageEncoder.encode(listOf(acRecord))

        val hsRecord = RawNdefRecord(
            tnf = Tnf.WELL_KNOWN,
            type = byteArrayOf('H'.code.toByte(), 's'.code.toByte()),
            payload = byteArrayOf(0x12) + embeddedMessage, // version 1.2
        )

        return listOf(hsRecord, carrierRecord)
    }

    /** Alternative Carrier record payload: CPS + carrier data reference + 0 auxiliary refs. */
    private fun buildAcPayload(carrierDataRef: String): ByteArray {
        val refBytes = carrierDataRef.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(0x01) + // CPS = 1 (active)
            byteArrayOf(refBytes.size.toByte()) + refBytes +
            byteArrayOf(0x00) // auxiliary data reference count = 0
    }
}
