package com.nfcemu.ndefengine

/**
 * Single entry point turning a [NdefPayload] (+ optional [AarConfig]) into the final
 * NDEF message bytes served by the HCE service. This is the only place that needs to
 * change when a new [NdefPayload] variant is added.
 */
object NdefMessageFactory {

    fun build(payload: NdefPayload, aar: AarConfig? = null): ByteArray {
        val records = encodePayloadRecords(payload).toMutableList()
        aar?.let { records += AarRecordEncoder.encode(it.packageName) }
        return NdefMessageEncoder.encode(records)
    }

    private fun encodePayloadRecords(payload: NdefPayload): List<RawNdefRecord> = when (payload) {
        is NdefPayload.Uri -> listOf(UriRecordEncoder.encode(payload.uri))
        is NdefPayload.Text -> listOf(TextRecordEncoder.encode(payload.text, payload.languageCode))
        is NdefPayload.VCard -> listOf(VCardRecordEncoder.encode(payload))
        is NdefPayload.WifiHandover -> WifiHandoverRecordEncoder.encode(payload)
        is NdefPayload.BluetoothHandover -> BluetoothHandoverRecordEncoder.encode(payload)
    }
}
