package com.nfcemu.data.export

import com.nfcemu.data.Profile
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ndefengine.NdefPayload
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryZipCodecTest {

    @Test
    fun `exporting then importing round-trips every profile`() {
        val profiles = listOf(
            Profile(name = "Website", fields = NdefPayload.Uri("https://example.com")),
            Profile(name = "Card", fields = NdefPayload.VCard(name = "Ada Lovelace")),
            Profile(name = "Note", fields = NdefPayload.Text("hello")),
        )
        val zipBytes = LibraryZipCodec.buildZip(profiles.map { it to NdefMessageFactory.build(it.fields) })

        val extracted = LibraryZipCodec.extractProfiles(zipBytes)

        assertEquals(0, extracted.skipped)
        assertEquals(profiles.map { it.fields }.toSet(), extracted.profiles.map { it.fields }.toSet())
        assertEquals(profiles.map { it.name }.toSet(), extracted.profiles.map { it.name }.toSet())
    }

    @Test
    fun `entries with identical names still both round-trip, distinguished by id suffix`() {
        val profiles = listOf(
            Profile(name = "Same Name", fields = NdefPayload.Text("a")),
            Profile(name = "Same Name", fields = NdefPayload.Text("b")),
        )
        val zipBytes = LibraryZipCodec.buildZip(profiles.map { it to NdefMessageFactory.build(it.fields) })

        val extracted = LibraryZipCodec.extractProfiles(zipBytes)

        assertEquals(0, extracted.skipped)
        assertEquals(2, extracted.profiles.size)
        assertEquals(setOf("a", "b"), extracted.profiles.map { (it.fields as NdefPayload.Text).text }.toSet())
    }

    @Test
    fun `a corrupt entry is skipped without aborting the rest of the import`() {
        val valid = Profile(name = "Valid", fields = NdefPayload.Text("ok"))
        val validJson = NfcEmuFileCodec.encode(valid, NdefMessageFactory.build(valid.fields))

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("valid.nfcemu"))
            zip.write(validJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("corrupt.nfcemu"))
            zip.write("this is not json at all {{{".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("also-corrupt.nfcemu"))
            zip.write("""{"formatVersion":1}""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val extracted = LibraryZipCodec.extractProfiles(out.toByteArray())

        assertEquals(1, extracted.profiles.size)
        assertEquals(2, extracted.skipped)
        assertEquals("Valid", extracted.profiles.single().name)
    }

    @Test
    fun `an empty zip extracts to no profiles and no skips`() {
        val emptyZip = ByteArrayOutputStream().also { ZipOutputStream(it).close() }.toByteArray()
        val extracted = LibraryZipCodec.extractProfiles(emptyZip)
        assertTrue(extracted.profiles.isEmpty())
        assertEquals(0, extracted.skipped)
    }
}
