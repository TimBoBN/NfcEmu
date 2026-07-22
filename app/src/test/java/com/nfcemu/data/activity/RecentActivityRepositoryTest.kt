package com.nfcemu.data.activity

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecentActivityRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: CoroutineScope
    private lateinit var repository: RecentActivityRepository

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        tempDir = File.createTempFile("nfcemu-test", "").apply {
            delete()
            mkdirs()
        }
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempDir, "recent_activity.preferences_pb") },
        )
        repository = RecentActivityRepository(RecentActivityDataStore(dataStore), scope)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `recording an entry prepends it to the list`() = runTest {
        repository.record("Website", "uri")
        repository.record("Business card", "vcard")

        val entries = repository.recent.first { it.size == 2 }
        assertEquals("Business card", entries.first().name)
        assertEquals("Website", entries.last().name)
    }

    @Test
    fun `recording more than 5 entries caps the list at the 5 most recent`() = runTest {
        repeat(7) { i -> repository.record("Profile $i", "text") }

        val entries = repository.recent.first { it.size == 5 }
        assertEquals("Profile 6", entries.first().name)
        assertEquals("Profile 2", entries.last().name)
    }
}
