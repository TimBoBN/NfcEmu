package com.nfcemu.ui.profileform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.data.local.ProfileDataStore
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ui.scantag.ScannedPayloadCodec
import com.nfcemu.ui.scantag.ScannedTag
import com.nfcemu.util.InstalledApp
import com.nfcemu.util.InstalledAppsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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

private object FakeInstalledAppsSource : InstalledAppsSource {
    override suspend fun queryLaunchableApps(): List<InstalledApp> = emptyList()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileFormViewModelTest {

    private lateinit var tempDir: File
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: ProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempDir = File.createTempFile("nfcemu-form-vm-test", "").apply { delete(); mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { File(tempDir, "profiles.preferences_pb") })
        repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        repository = ProfileRepository(ProfileDataStore(dataStore), repositoryScope)
    }

    @AfterTest
    fun tearDown() {
        repositoryScope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `new profile starts on the requested template with invalid, empty state`() {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "TEXT")))
        val state = viewModel.uiState.value
        assertEquals(ProfileTypeTemplate.TEXT, state.template)
        assertTrue(!state.isEditing)
        assertTrue(!state.isValid)
    }

    @Test
    fun `filling in a valid name and text makes the form valid and estimates a size`() {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "TEXT")))
        viewModel.updateName("My Text")
        viewModel.updateFields(ProfileFormFields.Text("Hello"))

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertTrue(state.estimatedNdefSize > 0)
        assertEquals("Hello", state.previewText)
    }

    @Test
    fun `saving a new profile creates it in the repository with the entered fields`() = runTest {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "WEBSITE")))
        viewModel.updateName("My Site")
        viewModel.updateFields(ProfileFormFields.Website("example.com"))

        viewModel.save()

        val profiles = repository.profiles.first { it.isNotEmpty() }
        val created = profiles.single()
        assertEquals("My Site", created.name)
        assertEquals(NdefPayload.Uri("https://example.com"), created.fields)
        assertNull(created.aarPackageName)
    }

    @Test
    fun `enabling aar with an invalid package name blocks saving`() {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "TEXT")))
        viewModel.updateName("X")
        viewModel.updateFields(ProfileFormFields.Text("hi"))
        viewModel.setAarEnabled(true)
        viewModel.updateAarPackageName("not a package")

        val state = viewModel.uiState.value
        assertTrue(state.aarError != null)
        assertTrue(!state.isValid)
    }

    @Test
    fun `enabling aar with a valid package name is included on save`() = runTest {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "TEXT")))
        viewModel.updateName("X")
        viewModel.updateFields(ProfileFormFields.Text("hi"))
        viewModel.setAarEnabled(true)
        viewModel.updateAarPackageName("com.example.app")

        assertTrue(viewModel.uiState.value.isValid)
        viewModel.save()

        val created = repository.profiles.first { it.isNotEmpty() }.single()
        assertEquals("com.example.app", created.aarPackageName)
    }

    @Test
    fun `blank profile name is invalid even with otherwise-valid fields`() {
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("template" to "TEXT")))
        viewModel.updateFields(ProfileFormFields.Text("hi"))
        assertTrue(viewModel.uiState.value.nameError != null)
        assertTrue(!viewModel.uiState.value.isValid)
    }

    @Test
    fun `editing an existing profile pre-fills the form and updates in place rather than duplicating`() = runTest {
        val existing = Profile(name = "Original", fields = NdefPayload.Text("v1"))
        repository.createProfile(existing)
        repository.profiles.first { it.isNotEmpty() }

        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("profileId" to existing.id)))
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isEditing)
        assertEquals("Original", initialState.name)
        assertEquals(ProfileTypeTemplate.TEXT, initialState.template)

        viewModel.updateFields(ProfileFormFields.Text("v2"))
        viewModel.save()

        val profiles = repository.profiles.first { list -> list.any { (it.fields as? NdefPayload.Text)?.text == "v2" } }
        assertEquals(1, profiles.size, "editing must not create a second profile")
        assertEquals(existing.id, profiles.single().id)
    }

    @Test
    fun `a scanned tag pre-fills the form as a new, unpinned profile rather than an edit`() {
        val scannedTag = ScannedTag(NdefPayload.Uri("https://example.com"), aarPackageName = null)
        val encoded = ScannedPayloadCodec.encode(scannedTag)

        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("scannedTag" to encoded)))
        val state = viewModel.uiState.value

        assertTrue(!state.isEditing)
        assertTrue(state.isScanned)
        assertEquals(ProfileTypeTemplate.WEBSITE, state.template)
        assertEquals(ProfileFormFields.Website("https://example.com"), state.fields)
        assertTrue(!state.aarEnabled)
    }

    @Test
    fun `a scanned tag with an aar record pre-fills and enables the aar section`() {
        val scannedTag = ScannedTag(NdefPayload.Text("hi"), aarPackageName = "com.example.app")
        val encoded = ScannedPayloadCodec.encode(scannedTag)

        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("scannedTag" to encoded)))
        val state = viewModel.uiState.value

        assertTrue(state.aarEnabled)
        assertEquals("com.example.app", state.aarPackageName)
    }

    @Test
    fun `saving a scanned tag creates a new profile, not an edit of anything existing`() = runTest {
        val scannedTag = ScannedTag(NdefPayload.Text("scanned text"), aarPackageName = null)
        val encoded = ScannedPayloadCodec.encode(scannedTag)
        val viewModel = ProfileFormViewModel(repository, FakeInstalledAppsSource, SavedStateHandle(mapOf("scannedTag" to encoded)))
        viewModel.updateName("My Scanned Tag")

        viewModel.save()

        val created = repository.profiles.first { it.isNotEmpty() }.single()
        assertEquals("My Scanned Tag", created.name)
        assertEquals(NdefPayload.Text("scanned text"), created.fields)
    }
}
