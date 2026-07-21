package com.nfcemu.ndefengine

/**
 * Android Application Record (AAR): an External Type record ("android.com:pkg") that
 * tells Android which app to launch/prioritize for this tag, even if another app is
 * also registered for the same NDEF content. Always appended as the last record.
 */
object AarRecordEncoder {

    private val AAR_TYPE = "android.com:pkg".toByteArray(Charsets.US_ASCII)

    fun encode(packageName: String): RawNdefRecord =
        RawNdefRecord(
            tnf = Tnf.EXTERNAL_TYPE,
            type = AAR_TYPE,
            payload = packageName.toByteArray(Charsets.UTF_8),
        )
}
