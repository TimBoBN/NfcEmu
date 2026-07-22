package com.nfcemu.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.local.ProfileDataStore
import com.nfcemu.ndefengine.NdefPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises [ProfileRepository] against a real, temp-file-backed Preferences
 * DataStore (no Android Context / Robolectric needed - see [com.nfcemu.di.DataStoreModule]
 * kdoc for why the DataStore instance is constructor-injected rather than derived
 * from Context internally).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: ProfileRepository

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        tempDir = File.createTempFile("nfcemu-test", "").apply {
            delete()
            mkdirs()
        }
        // Built on the same dispatcher installed as Main above (not Dispatchers.Default), so
        // runTest's virtual scheduler can see and deterministically drive this scope's work -
        // otherwise repository/DataStore internals run on a real, untracked dispatcher that
        // can race against runTest's own end-of-test bookkeeping under load.
        repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = repositoryScope,
            produceFile = { File(tempDir, "profiles.preferences_pb") },
        )
        repository = ProfileRepository(ProfileDataStore(dataStore), repositoryScope)
    }

    @AfterTest
    fun tearDown() {
        repositoryScope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `creating a profile adds it to the list`() = runTest {
        val profile = Profile(name = "Website", fields = NdefPayload.Uri("https://example.com"))
        repository.createProfile(profile)

        val profiles = repository.profiles.first { it.isNotEmpty() }
        assertEquals(1, profiles.size)
        assertEquals("Website", profiles.single().name)
    }

    @Test
    fun `setting a profile active updates currentNdefBytes with its encoded content`() = runTest {
        val profile = Profile(name = "Text", fields = NdefPayload.Text("hello-marker"))
        repository.createProfile(profile)
        repository.setActive(profile.id)

        val ndefBytes = repository.currentNdefBytes.first { it.isNotEmpty() }
        assertTrue(String(ndefBytes, Charsets.UTF_8).contains("hello-marker"))
        assertEquals(profile.id, repository.activeProfileId.first { it == profile.id })
    }

    @Test
    fun `editing an inactive profile does not touch currentNdefBytes`() = runTest {
        val active = Profile(name = "Active", fields = NdefPayload.Text("active-marker"))
        val other = Profile(name = "Other", fields = NdefPayload.Text("other-marker"))
        repository.createProfile(active)
        repository.createProfile(other)
        repository.setActive(active.id)
        repository.currentNdefBytes.first { String(it, Charsets.UTF_8).contains("active-marker") }

        repository.updateProfile(other.copy(name = "Other renamed"))
        repository.profiles.first { list -> list.any { it.name == "Other renamed" } }

        val stillActiveBytes = repository.currentNdefBytes.first()
        assertTrue(String(stillActiveBytes, Charsets.UTF_8).contains("active-marker"))
    }

    @Test
    fun `editing the active profile re-encodes currentNdefBytes`() = runTest {
        val profile = Profile(name = "Active", fields = NdefPayload.Text("version-1"))
        repository.createProfile(profile)
        repository.setActive(profile.id)
        repository.currentNdefBytes.first { String(it, Charsets.UTF_8).contains("version-1") }

        repository.updateProfile(profile.copy(fields = NdefPayload.Text("version-2")))

        val updatedBytes = repository.currentNdefBytes.first { String(it, Charsets.UTF_8).contains("version-2") }
        assertTrue(String(updatedBytes, Charsets.UTF_8).contains("version-2"))
    }

    @Test
    fun `deleting the active profile clears active id and current bytes`() = runTest {
        val profile = Profile(name = "ToDelete", fields = NdefPayload.Text("marker"))
        repository.createProfile(profile)
        repository.setActive(profile.id)
        repository.currentNdefBytes.first { it.isNotEmpty() }

        repository.deleteProfile(profile.id)

        assertNull(repository.activeProfileId.first { it == null })
        val bytes = repository.currentNdefBytes.first { it.isEmpty() }
        assertTrue(bytes.isEmpty())
        assertTrue(repository.profiles.first { it.isEmpty() }.isEmpty())
    }

    @Test
    fun `duplicating a profile creates an unpinned copy with a new id and a suffixed name`() = runTest {
        val original = Profile(name = "Original", fields = NdefPayload.Text("x"), pinned = true)
        repository.createProfile(original)

        val copy = repository.duplicateProfile(original.id)

        assertTrue(copy != null)
        assertTrue(copy.id != original.id)
        assertEquals("Original (Copy)", copy.name)
        assertEquals(false, copy.pinned)
        assertEquals(2, repository.profiles.first { it.size == 2 }.size)
    }

    @Test
    fun `toggling pinned flips the flag without affecting other profiles`() = runTest {
        val a = Profile(name = "A", fields = NdefPayload.Text("a"), pinned = false)
        val b = Profile(name = "B", fields = NdefPayload.Text("b"), pinned = false)
        repository.createProfile(a)
        repository.createProfile(b)
        repository.profiles.first { it.size == 2 }

        repository.togglePinned(a.id)

        val profiles = repository.profiles.first { list -> list.any { it.id == a.id && it.pinned } }
        assertEquals(true, profiles.first { it.id == a.id }.pinned)
        assertEquals(false, profiles.first { it.id == b.id }.pinned)
    }

    @Test
    fun `importing a profile assigns a fresh id and never sets it active`() = runTest {
        val existing = Profile(name = "Existing", fields = NdefPayload.Text("existing"))
        repository.createProfile(existing)
        repository.setActive(existing.id)
        repository.activeProfileId.first { it == existing.id }

        val imported = repository.importProfile(Profile(name = "Imported", fields = NdefPayload.Text("imported")))

        assertTrue(imported.id.isNotBlank())
        val profiles = repository.profiles.first { it.size == 2 }
        assertTrue(profiles.any { it.id == imported.id })
        assertEquals(existing.id, repository.activeProfileId.first())
    }
}
