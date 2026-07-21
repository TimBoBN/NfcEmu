package com.nfcemu.ui.profileform

import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ndefengine.WifiAuthType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileFormCodecTest {

    // --- Website ---

    @Test
    fun `blank website url is invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Website("")).isValid)
    }

    @Test
    fun `website without scheme gets https prepended`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.Website("example.com")) as NdefPayload.Uri
        assertEquals("https://example.com", payload.uri)
    }

    @Test
    fun `website with explicit scheme is kept as-is`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.Website("http://example.com")) as NdefPayload.Uri
        assertEquals("http://example.com", payload.uri)
    }

    // --- Phone ---

    @Test
    fun `phone number requires at least one digit`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Phone("abc")).isValid)
        assertTrue(ProfileFormCodec.validate(ProfileFormFields.Phone("+49 170 1234567")).isValid)
    }

    @Test
    fun `phone number strips whitespace into a tel uri`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.Phone("+49 170 1234567")) as NdefPayload.Uri
        assertEquals("tel:+491701234567", payload.uri)
    }

    // --- Email ---

    @Test
    fun `email without at-sign is invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Email("not-an-email")).isValid)
    }

    @Test
    fun `email without dot after at-sign is invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Email("test@localhost")).isValid)
    }

    @Test
    fun `valid email with subject and body builds a properly encoded mailto uri`() {
        val fields = ProfileFormFields.Email("test@example.com", subject = "Hallo Welt", body = "Wie geht's?")
        assertTrue(ProfileFormCodec.validate(fields).isValid)
        val payload = ProfileFormCodec.toPayload(fields) as NdefPayload.Uri
        assertEquals("mailto:test@example.com?subject=Hallo%20Welt&body=Wie%20geht%27s%3F", payload.uri)
    }

    @Test
    fun `email without subject or body omits the query string entirely`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.Email("test@example.com")) as NdefPayload.Uri
        assertEquals("mailto:test@example.com", payload.uri)
    }

    // --- Sms ---

    @Test
    fun `sms requires a numeric number`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Sms("not-a-number")).isValid)
        assertTrue(ProfileFormCodec.validate(ProfileFormFields.Sms("+491701234567")).isValid)
    }

    // --- Location ---

    @Test
    fun `latitude and longitude out of range are both reported`() {
        val result = ProfileFormCodec.validate(ProfileFormFields.Location("200", "-200"))
        assertTrue(!result.isValid)
        assertTrue("latitude" in result.errors)
        assertTrue("longitude" in result.errors)
    }

    @Test
    fun `valid coordinates build a geo uri`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.Location("52.52", "13.405")) as NdefPayload.Uri
        assertEquals("geo:52.52,13.405", payload.uri)
    }

    @Test
    fun `non-numeric coordinates are invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Location("abc", "13.4")).isValid)
    }

    // --- PlayStore ---

    @Test
    fun `play store app id must look like a package name`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.PlayStore("not valid!")).isValid)
        assertTrue(ProfileFormCodec.validate(ProfileFormFields.PlayStore("com.example.app")).isValid)
    }

    @Test
    fun `play store id builds a market uri`() {
        val payload = ProfileFormCodec.toPayload(ProfileFormFields.PlayStore("com.example.app")) as NdefPayload.Uri
        assertEquals("market://details?id=com.example.app", payload.uri)
    }

    // --- Custom Uri ---

    @Test
    fun `custom uri requires a scheme separator`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.CustomUri("no-scheme-here")).isValid)
        assertTrue(ProfileFormCodec.validate(ProfileFormFields.CustomUri("myapp://open")).isValid)
    }

    // --- Wifi ---

    @Test
    fun `open wifi network does not require a password`() {
        val fields = ProfileFormFields.Wifi(ssid = "OpenNet", authType = WifiAuthType.OPEN, password = "")
        assertTrue(ProfileFormCodec.validate(fields).isValid)
    }

    @Test
    fun `wpa2 network requires a password of at least 8 characters`() {
        val tooShort = ProfileFormFields.Wifi(ssid = "Net", authType = WifiAuthType.WPA2_PSK, password = "short")
        val result = ProfileFormCodec.validate(tooShort)
        assertTrue(!result.isValid)
        assertTrue("password" in result.errors)
    }

    @Test
    fun `wep network accepts short passwords since wep keys can be 5 or 13 chars`() {
        val fields = ProfileFormFields.Wifi(ssid = "Net", authType = WifiAuthType.WEP, password = "abcde")
        assertTrue(ProfileFormCodec.validate(fields).isValid)
    }

    @Test
    fun `blank ssid is always invalid regardless of auth type`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Wifi(ssid = "", authType = WifiAuthType.OPEN)).isValid)
    }

    // --- VCard ---

    @Test
    fun `completely empty vcard is invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.VCard()).isValid)
    }

    @Test
    fun `vcard with only an organization filled in is valid`() {
        assertTrue(ProfileFormCodec.validate(ProfileFormFields.VCard(organization = "Acme Inc.")).isValid)
    }

    // --- Text ---

    @Test
    fun `blank text is invalid`() {
        assertTrue(!ProfileFormCodec.validate(ProfileFormFields.Text("")).isValid)
    }

    // --- Round-trip (toFormFields is the inverse of toPayload for edit mode) ---

    @Test
    fun `tel uri round-trips back into the phone template`() {
        val fields = ProfileFormCodec.toFormFields(NdefPayload.Uri("tel:+491701234567"))
        assertTrue(fields is ProfileFormFields.Phone)
        assertEquals("+491701234567", fields.number)
    }

    @Test
    fun `mailto uri with query round-trips subject and body`() {
        val fields = ProfileFormCodec.toFormFields(NdefPayload.Uri("mailto:test@example.com?subject=Hi&body=Hello%20there"))
        assertTrue(fields is ProfileFormFields.Email)
        assertEquals("test@example.com", fields.address)
        assertEquals("Hi", fields.subject)
        assertEquals("Hello there", fields.body)
    }

    @Test
    fun `geo uri round-trips into latitude and longitude`() {
        val fields = ProfileFormCodec.toFormFields(NdefPayload.Uri("geo:52.52,13.405"))
        assertTrue(fields is ProfileFormFields.Location)
        assertEquals("52.52", fields.latitude)
        assertEquals("13.405", fields.longitude)
    }

    @Test
    fun `unrecognized custom scheme round-trips into the custom uri template`() {
        val fields = ProfileFormCodec.toFormFields(NdefPayload.Uri("myapp://deep/link"))
        assertTrue(fields is ProfileFormFields.CustomUri)
        assertEquals("myapp://deep/link", fields.uri)
    }

    @Test
    fun `wifi payload round-trips all fields including password`() {
        val fields = ProfileFormCodec.toFormFields(NdefPayload.WifiHandover("Net", WifiAuthType.WPA2_PSK, "supersecret"))
        assertTrue(fields is ProfileFormFields.Wifi)
        assertEquals("Net", fields.ssid)
        assertEquals(WifiAuthType.WPA2_PSK, fields.authType)
        assertEquals("supersecret", fields.password)
    }
}
