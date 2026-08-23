package com.nfcemu.ndefengine

/**
 * STRETCH GOAL - encodes Wi-Fi credentials as an NFC Forum Connection Handover
 * "Handover Select" message carrying a single Wi-Fi Simple Config (WSC) carrier,
 * the same structure used by "Wi-Fi config token" NFC tags:
 *
 * ```
 * NDEF message:
 *   [0] "Hs" record (Well Known)   - version byte + embedded message with one "ac" record
 *   [1] "application/vnd.wfa.wsc"  - MIME record, id="0", payload = WSC Credential TLV
 * ```
 *
 * This has not been validated against a hardware Wi-Fi Direct/NFC reader; treat it as
 * best-effort interop with apps that specifically parse WSC-over-NFC tokens (e.g.
 * NFC Tools "Wi-Fi network" record type). It is intentionally isolated in its own
 * encoder so a gap here never blocks the other, spec-solid record types.
 */
object WifiHandoverRecordEncoder {

    private const val CARRIER_REF = "0"

    // WSC (Wi-Fi Simple Config) attribute IDs, WFA WSC 2.0 spec section 12.
    private const val ATTR_CREDENTIAL = 0x100E
    private const val ATTR_SSID = 0x1045
    private const val ATTR_AUTH_TYPE = 0x1003
    private const val ATTR_ENCRYPTION_TYPE = 0x100F
    private const val ATTR_NETWORK_KEY = 0x1027
    private const val ATTR_MAC_ADDRESS = 0x1020

    private const val AUTH_OPEN = 0x0001
    private const val AUTH_WPA_PSK = 0x0002
    private const val AUTH_WPA2_PSK = 0x0020

    private const val ENC_NONE = 0x0001
    private const val ENC_WEP = 0x0002
    private const val ENC_TKIP = 0x0004
    private const val ENC_AES = 0x0008

    fun encode(wifi: NdefPayload.WifiHandover): List<RawNdefRecord> {
        val carrierRecord = RawNdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "application/vnd.wfa.wsc".toByteArray(Charsets.US_ASCII),
            id = CARRIER_REF.toByteArray(Charsets.US_ASCII),
            payload = buildCredentialTlv(wifi),
        )
        return ConnectionHandoverSupport.buildHandoverMessage(carrierRecord, CARRIER_REF)
    }

    private fun buildCredentialTlv(wifi: NdefPayload.WifiHandover): ByteArray {
        val ssidBytes = wifi.ssid.toByteArray(Charsets.UTF_8)
        val (authType, encType) = when (wifi.authType) {
            WifiAuthType.OPEN -> AUTH_OPEN to ENC_NONE
            WifiAuthType.WEP -> AUTH_OPEN to ENC_WEP
            WifiAuthType.WPA_PSK -> AUTH_WPA_PSK to ENC_TKIP
            WifiAuthType.WPA2_PSK -> AUTH_WPA2_PSK to ENC_AES
        }
        val keyBytes = wifi.password?.takeIf { it.isNotEmpty() && wifi.authType != WifiAuthType.OPEN }
            ?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)

        var body = tlv(ATTR_SSID, ssidBytes)
        body += tlv16(ATTR_AUTH_TYPE, authType)
        body += tlv16(ATTR_ENCRYPTION_TYPE, encType)
        if (keyBytes.isNotEmpty()) {
            body += tlv(ATTR_NETWORK_KEY, keyBytes)
        }
        body += tlv(ATTR_MAC_ADDRESS, byteArrayOf(-1, -1, -1, -1, -1, -1)) // wildcard MAC

        return tlv(ATTR_CREDENTIAL, body)
    }

    private fun tlv(attributeId: Int, value: ByteArray): ByteArray {
        require(value.size <= 0xFFFF) { "WSC TLV value too long" }
        return byteArrayOf(
            (attributeId shr 8).toByte(), (attributeId and 0xFF).toByte(),
            (value.size shr 8).toByte(), (value.size and 0xFF).toByte(),
        ) + value
    }

    private fun tlv16(attributeId: Int, value: Int): ByteArray =
        tlv(attributeId, byteArrayOf((value shr 8).toByte(), (value and 0xFF).toByte()))
}
