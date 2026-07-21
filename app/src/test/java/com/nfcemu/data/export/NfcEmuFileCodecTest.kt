package com.nfcemu.data.export

import com.nfcemu.data.Profile
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ndefengine.NdefPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NfcEmuFileCodecTest {

    @Test
    fun `encode then decode round-trips name, fields and aar package`() {
        val profile = Profile(
            name = "Meine Visitenkarte",
            fields = NdefPayload.VCard(name = "Ada Lovelace", phones = listOf("+491701234567")),
            aarPackageName = "com.example.myapp",
        )
        val ndefBytes = NdefMessageFactory.build(profile.fields, com.nfcemu.ndefengine.AarConfig(profile.aarPackageName!!))

        val json = NfcEmuFileCodec.encode(profile, ndefBytes)
        val decoded = NfcEmuFileCodec.decode(json)

        assertEquals(profile.name, decoded.profile.name)
        assertEquals(profile.fields, decoded.profile.fields)
        assertEquals(profile.aarPackageName, decoded.profile.aarPackageName)
        assertTrue(decoded.rawNdefBytes.contentEquals(ndefBytes))
    }

    @Test
    fun `round-trips every payload type`() {
        val payloads = listOf(
            NdefPayload.Uri("https://example.com"),
            NdefPayload.Text("Hallo Welt", "de"),
            NdefPayload.WifiHandover("HomeNet", com.nfcemu.ndefengine.WifiAuthType.WPA2_PSK, "secret123"),
            NdefPayload.VCard(),
        )
        for (fields in payloads) {
            val profile = Profile(name = "Test", fields = fields)
            val bytes = NdefMessageFactory.build(fields)
            val json = NfcEmuFileCodec.encode(profile, bytes)
            val decoded = NfcEmuFileCodec.decode(json)
            assertEquals(fields, decoded.profile.fields)
        }
    }

    @Test
    fun `profile without aar package round-trips as null, not a literal string`() {
        val profile = Profile(name = "No AAR", fields = NdefPayload.Text("x"))
        val json = NfcEmuFileCodec.encode(profile, NdefMessageFactory.build(profile.fields))
        val decoded = NfcEmuFileCodec.decode(json)
        assertEquals(null, decoded.profile.aarPackageName)
    }

    @Test
    fun `garbage input is rejected as corrupt, not an uncaught exception`() {
        assertFailsWith<NfcEmuFileException.Corrupt> {
            NfcEmuFileCodec.decode("this is not json at all {{{")
        }
    }

    @Test
    fun `valid json missing required fields is rejected as corrupt`() {
        assertFailsWith<NfcEmuFileException.Corrupt> {
            NfcEmuFileCodec.decode("""{"formatVersion": 1}""")
        }
    }

    @Test
    fun `blank profile name is rejected as corrupt`() {
        val malformed = """
            {"formatVersion":1,"exportedAt":1,"profile":{"name":"   ","fields":{"type":"text","text":"x","languageCode":"de"}},"ndefBase64":"AA=="}
        """.trimIndent()
        assertFailsWith<NfcEmuFileException.Corrupt> {
            NfcEmuFileCodec.decode(malformed)
        }
    }

    @Test
    fun `invalid base64 ndef payload is rejected as corrupt rather than crashing`() {
        val malformed = """
            {"formatVersion":1,"exportedAt":1,"profile":{"name":"X","fields":{"type":"text","text":"x","languageCode":"de"}},"ndefBase64":"not-valid-base64!!!"}
        """.trimIndent()
        assertFailsWith<NfcEmuFileException.Corrupt> {
            NfcEmuFileCodec.decode(malformed)
        }
    }

    @Test
    fun `future format version is rejected with an unsupported-version error`() {
        val futureVersion = """
            {"formatVersion":999,"exportedAt":1,"profile":{"name":"X","fields":{"type":"text","text":"x","languageCode":"de"}},"ndefBase64":"AA=="}
        """.trimIndent()
        assertFailsWith<NfcEmuFileException.UnsupportedVersion> {
            NfcEmuFileCodec.decode(futureVersion)
        }
    }

    @Test
    fun `unknown extra json fields are tolerated for forward compatibility`() {
        val withExtraField = """
            {"formatVersion":1,"exportedAt":1,"profile":{"name":"X","fields":{"type":"text","text":"x","languageCode":"de"},"futureField":"ignored"},"ndefBase64":"AA==","anotherFutureField":123}
        """.trimIndent()
        val decoded = NfcEmuFileCodec.decode(withExtraField)
        assertEquals("X", decoded.profile.name)
    }

    @Test
    fun `unknown payload type discriminator is rejected as corrupt`() {
        val unknownType = """
            {"formatVersion":1,"exportedAt":1,"profile":{"name":"X","fields":{"type":"holobeam","data":"x"}},"ndefBase64":"AA=="}
        """.trimIndent()
        assertFailsWith<NfcEmuFileException.Corrupt> {
            NfcEmuFileCodec.decode(unknownType)
        }
    }
}
