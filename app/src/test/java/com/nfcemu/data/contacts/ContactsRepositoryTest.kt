package com.nfcemu.data.contacts

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
class ContactsRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: CoroutineScope
    private lateinit var repository: ContactsRepository

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
            produceFile = { File(tempDir, "contacts.preferences_pb") },
        )
        repository = ContactsRepository(ContactsDataStore(dataStore), scope)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `adding a contact prepends it to the list`() = runTest {
        repository.add(Contact(name = "Max Mustermann", phone = "+49 151 2345678"))
        repository.add(Contact(name = "Anna Weber", email = "anna@weber.dev"))

        val contacts = repository.contacts.first { it.size == 2 }
        assertEquals("Anna Weber", contacts.first().name)
        assertEquals("Max Mustermann", contacts.last().name)
    }
}
