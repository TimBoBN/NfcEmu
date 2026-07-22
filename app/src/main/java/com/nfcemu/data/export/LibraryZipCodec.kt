package com.nfcemu.data.export

import com.nfcemu.data.Profile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** How many of a ZIP's entries decoded into a usable profile. */
data class ExtractedProfiles(val profiles: List<Profile>, val skipped: Int)

/**
 * ZIP-of-individual-`.nfcemu`-files bulk export/import format: no new combined schema, each
 * entry is exactly what [NfcEmuFileCodec.encode] produces for a single profile, so a single
 * entry extracted from the ZIP re-imports like any other `.nfcemu` file. Pure Kotlin/JVM
 * (operates on [ByteArray], no `Context`/`Uri`) so it's testable as plain JUnit, the same way
 * [NfcEmuFileCodec] is - [com.nfcemu.data.FileRepository] is just the thin SAF I/O wrapper
 * around this.
 */
object LibraryZipCodec {

    fun buildZip(entries: List<Pair<Profile, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((profile, ndefBytes) in entries) {
                val json = NfcEmuFileCodec.encode(profile, ndefBytes)
                zip.putNextEntry(ZipEntry(entryName(profile)))
                zip.write(json.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** Each entry is decoded independently: one corrupt/foreign entry only adds to [ExtractedProfiles.skipped]. */
    fun extractProfiles(zipBytes: ByteArray): ExtractedProfiles {
        val profiles = mutableListOf<Profile>()
        var skipped = 0
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.getNextEntry()
            while (entry != null) {
                if (!entry.isDirectory) {
                    runCatching { NfcEmuFileCodec.decode(String(zip.readBytes(), Charsets.UTF_8)).profile }
                        .onSuccess(profiles::add)
                        .onFailure { skipped++ }
                }
                zip.closeEntry()
                entry = zip.getNextEntry()
            }
        }
        return ExtractedProfiles(profiles, skipped)
    }

    private fun entryName(profile: Profile): String {
        val sanitized = profile.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "profile" }
        return "$sanitized-${profile.id.take(8)}.nfcemu"
    }
}
