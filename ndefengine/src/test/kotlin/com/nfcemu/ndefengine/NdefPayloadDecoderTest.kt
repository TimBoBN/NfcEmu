package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NdefPayloadDecoderTest {

    @Test
    fun `empty bytes decode as empty`() {
        assertEquals(DecodedTagResult.Empty, NdefPayloadDecoder.decode(ByteArray(0)))
    }

    @Test
    fun `truncated garbage decodes as unsupported instead of throwing`() {
        val result = NdefPayloadDecoder.decode(byteArrayOf(0x03, 0x10, 0x55))
        assertIs<DecodedTagResult.Unsupported>(result)
    }

    @Test
    fun `every supported payload type round-trips through encode then decode`() {
        val payloads = listOf(
            NdefPayload.Uri("tel:+491701234567"),
            NdefPayload.Uri("mailto:test@example.com?subject=Hi"),
            NdefPayload.Uri("sms:+491701234567?body=Hallo"),
            NdefPayload.Uri("geo:52.5200,13.4050"),
            NdefPayload.Uri("market://details?id=com.example.app"),
            NdefPayload.Uri("https://example.com"),
            NdefPayload.VCard(
                name = "Edge Case",
                phones = listOf("+491701234567"),
                emails = listOf("edge@example.com"),
                organization = "Acme, Inc; Division",
                title = "Engineer",
                website = "https://example.com",
                address = "123 Main St, Apt 4",
            ),
            NdefPayload.Text(""),
            NdefPayload.Text("emoji test 😀🚀", languageCode = "en"),
            NdefPayload.WifiHandover("Empty", WifiAuthType.OPEN),
            NdefPayload.WifiHandover("Wep", WifiAuthType.WEP, "12345"),
            NdefPayload.WifiHandover("Wpa", WifiAuthType.WPA_PSK, "password1"),
            NdefPayload.WifiHandover("Wpa2", WifiAuthType.WPA2_PSK, "password1"),
        )

        for (payload in payloads) {
            val bytes = NdefMessageFactory.build(payload)
            val result = NdefPayloadDecoder.decode(bytes)
            val success = assertIs<DecodedTagResult.Success>(result, "payload=$payload")
            assertEquals(payload, success.payload, "round-trip mismatch for $payload")
            assertNull(success.aarPackageName)
        }
    }

    @Test
    fun `a trailing aar record is decoded separately from the primary content`() {
        val bytes = NdefMessageFactory.build(NdefPayload.Text("Hi"), aar = AarConfig("com.example.myapp"))
        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        assertEquals(NdefPayload.Text("Hi"), result.payload)
        assertEquals("com.example.myapp", result.aarPackageName)
    }

    @Test
    fun `wifi handover with aar round-trips both the credentials and the package name`() {
        val bytes = NdefMessageFactory.build(
            NdefPayload.WifiHandover("Net", WifiAuthType.WPA2_PSK, "password1"),
            aar = AarConfig("com.example.myapp"),
        )
        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        assertEquals(NdefPayload.WifiHandover("Net", WifiAuthType.WPA2_PSK, "password1"), result.payload)
        assertEquals("com.example.myapp", result.aarPackageName)
    }

    @Test
    fun `a tag with only an aar record and no readable content is unsupported`() {
        val aarOnly = NdefMessageEncoder.encode(listOf(AarRecordEncoder.encode("com.example.myapp")))
        assertIs<DecodedTagResult.Unsupported>(NdefPayloadDecoder.decode(aarOnly))
    }

    @Test
    fun `a foreign vcard not produced by this app's own encoder is parsed leniently`() {
        // Real-world conventions: CRLF line endings, folded ADR continuation line, an
        // unrecognized PHOTO property that must be skipped rather than aborting the parse.
        val vcard = "BEGIN:VCARD\r\n" +
            "VERSION:3.0\r\n" +
            "FN:Jane Doe\r\n" +
            "N:Doe;Jane;;;\r\n" +
            "TEL;TYPE=WORK,VOICE:+1 555 0100\r\n" +
            "TEL;TYPE=CELL:+1 555 0101\r\n" +
            "EMAIL;TYPE=INTERNET:jane@example.com\r\n" +
            "ADR;TYPE=WORK:;;123 Main St\r\n" +
            " , Suite 5;Springfield;IL;62701;USA\r\n" +
            "PHOTO;ENCODING=b;TYPE=JPEG:/9j/4AAQSkZJRgABAQAAAQABAAD==\r\n" +
            "END:VCARD\r\n"
        val record = RawNdefRecord(
            tnf = Tnf.MIME_MEDIA,
            type = "text/vcard".toByteArray(Charsets.US_ASCII),
            payload = vcard.toByteArray(Charsets.UTF_8),
        )
        val bytes = NdefMessageEncoder.encode(listOf(record))

        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        val decoded = assertIs<NdefPayload.VCard>(result.payload)
        assertEquals("Jane Doe", decoded.name)
        assertEquals(listOf("+1 555 0100", "+1 555 0101"), decoded.phones)
        assertEquals(listOf("jane@example.com"), decoded.emails)
        assertTrue(decoded.address!!.contains("123 Main St, Suite 5"))
    }

    @Test
    fun `an unrecognized record type is unsupported rather than throwing`() {
        val record = RawNdefRecord(tnf = Tnf.UNKNOWN, type = ByteArray(0), payload = byteArrayOf(1, 2, 3))
        val bytes = NdefMessageEncoder.encode(listOf(record))
        assertIs<DecodedTagResult.Unsupported>(NdefPayloadDecoder.decode(bytes))
    }

    @Test
    fun `a smart poster's wrapped uri is unwrapped and decoded`() {
        // RTD-SP: the "Sp" record's own payload is a nested NDEF message, here a title Text
        // record (decorative, this app has no field for it) followed by the mandatory Uri record.
        val innerMessage = NdefMessageEncoder.encode(
            listOf(
                TextRecordEncoder.encode("Acme Website"),
                UriRecordEncoder.encode("https://acme.example.com"),
            ),
        )
        val smartPoster = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = "Sp".toByteArray(Charsets.US_ASCII), payload = innerMessage)
        val bytes = NdefMessageEncoder.encode(listOf(smartPoster))

        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        assertEquals(NdefPayload.Uri("https://acme.example.com"), result.payload)
    }

    @Test
    fun `a smart poster without a uri record is unsupported`() {
        val innerMessage = NdefMessageEncoder.encode(listOf(TextRecordEncoder.encode("Title only, no link")))
        val smartPoster = RawNdefRecord(tnf = Tnf.WELL_KNOWN, type = "Sp".toByteArray(Charsets.US_ASCII), payload = innerMessage)
        val bytes = NdefMessageEncoder.encode(listOf(smartPoster))

        assertIs<DecodedTagResult.Unsupported>(NdefPayloadDecoder.decode(bytes))
    }

    @Test
    fun `an unrecognized leading record does not hide a recognized record behind it`() {
        val unrecognized = RawNdefRecord(tnf = Tnf.UNKNOWN, type = ByteArray(0), payload = byteArrayOf(9, 9))
        val bytes = NdefMessageEncoder.encode(listOf(unrecognized, UriRecordEncoder.encode("https://example.com")))

        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        assertEquals(NdefPayload.Uri("https://example.com"), result.payload)
    }

    @Test
    fun `a uri record followed by a text record only surfaces the first recognized record`() {
        val bytes = NdefMessageEncoder.encode(
            listOf(UriRecordEncoder.encode("https://example.com"), TextRecordEncoder.encode("secondary text")),
        )

        val result = assertIs<DecodedTagResult.Success>(NdefPayloadDecoder.decode(bytes))
        assertEquals(NdefPayload.Uri("https://example.com"), result.payload)
    }
}
