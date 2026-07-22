package com.nfcemu.ndefengine

/**
 * Inverse of [WifiHandoverRecordEncoder]'s WSC Credential TLV. Inherits that encoder's
 * "stretch goal, unverified against real hardware" caveat - a TLV shape this doesn't
 * recognize degrades to `null` (the caller surfaces that as "unsupported") rather than
 * throwing.
 */
object WifiHandoverRecordDecoder {

    private const val ATTR_CREDENTIAL = 0x100E
    private const val ATTR_SSID = 0x1045
    private const val ATTR_AUTH_TYPE = 0x1003
    private const val ATTR_ENCRYPTION_TYPE = 0x100F
    private const val ATTR_NETWORK_KEY = 0x1027

    private const val AUTH_WPA_PSK = 0x0002
    private const val AUTH_WPA2_PSK = 0x0020
    private const val ENC_WEP = 0x0002

    /** [wscCarrierPayload] is the MIME carrier record's payload, i.e. the credential TLV. */
    fun decode(wscCarrierPayload: ByteArray): NdefPayload.WifiHandover? {
        val credentialBytes = parseTlvs(wscCarrierPayload)[ATTR_CREDENTIAL] ?: return null
        val fields = parseTlvs(credentialBytes)

        val ssid = fields[ATTR_SSID]?.let { String(it, Charsets.UTF_8) } ?: return null
        val authTypeValue = fields[ATTR_AUTH_TYPE]?.takeIf { it.size >= 2 }?.let { be16(it) }
        val encTypeValue = fields[ATTR_ENCRYPTION_TYPE]?.takeIf { it.size >= 2 }?.let { be16(it) }
        val authType = when {
            authTypeValue == AUTH_WPA_PSK -> WifiAuthType.WPA_PSK
            authTypeValue == AUTH_WPA2_PSK -> WifiAuthType.WPA2_PSK
            encTypeValue == ENC_WEP -> WifiAuthType.WEP
            else -> WifiAuthType.OPEN
        }
        val password = fields[ATTR_NETWORK_KEY]?.takeIf { it.isNotEmpty() }?.let { String(it, Charsets.UTF_8) }

        return NdefPayload.WifiHandover(ssid = ssid, authType = authType, password = password)
    }

    /** Reads sequential (id: 2 bytes, length: 2 bytes, value) TLVs; stops at the first truncated one. */
    private fun parseTlvs(data: ByteArray): Map<Int, ByteArray> {
        val result = mutableMapOf<Int, ByteArray>()
        var offset = 0
        while (offset + 4 <= data.size) {
            val id = be16(data, offset)
            val length = be16(data, offset + 2)
            val valueStart = offset + 4
            if (valueStart + length > data.size) break
            result[id] = data.copyOfRange(valueStart, valueStart + length)
            offset = valueStart + length
        }
        return result
    }

    private fun be16(data: ByteArray, offset: Int = 0): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
}
