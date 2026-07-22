package com.nfcemu.ui.writetag

import android.app.Activity
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.data.local.ProfileDataStore
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.nfc.TagWriteResult
import com.nfcemu.nfc.TagWriterSource
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
import kotlin.test.assertIs

private class FakeTagWriterSource : TagWriterSource {
    override fun startWriting(activity: Activity, ndefBytes: ByteArray, onResult: (TagWriteResult) -> Unit) = Unit
    override fun stopWriting(activity: Activity) = Unit
}

/**
 * Exercises [WriteTagViewModel.onWriteResult] directly (it's `internal` for exactly this
 * reason - see its kdoc) rather than through [WriteTagViewModel.startWriting], since a plain
 * JUnit test (no Robolectric) can't construct a real [Activity] to pass through to
 * [TagWriterSource]. The profile lookup is exercised against a real, temp-file-backed
 * [ProfileRepository], same setup as [com.nfcemu.ui.profileform.ProfileFormViewModelTest].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WriteTagViewModelTest {

    private lateinit var tempDir: File
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: ProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempDir = File.createTempFile("nfcemu-write-tag-vm-test", "").apply { delete(); mkdirs() }
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
    fun `an unknown profile id surfaces as profile-not-found`() {
        val viewModel = WriteTagViewModel(FakeTagWriterSource(), repository, SavedStateHandle(mapOf("profileId" to "does-not-exist")))
        assertIs<WriteTagUiState.ProfileNotFound>(viewModel.uiState.value)
    }

    @Test
    fun `an existing profile starts in the waiting state`() = runTest {
        val profile = Profile(name = "My Site", fields = NdefPayload.Uri("https://example.com"))
        repository.createProfile(profile)
        repository.profiles.first { it.isNotEmpty() }

        val viewModel = WriteTagViewModel(FakeTagWriterSource(), repository, SavedStateHandle(mapOf("profileId" to profile.id)))

        assertIs<WriteTagUiState.Waiting>(viewModel.uiState.value)
    }

    @Test
    fun `a successful write surfaces as success`() = runTest {
        val profile = Profile(name = "My Site", fields = NdefPayload.Text("hi"))
        repository.createProfile(profile)
        repository.profiles.first { it.isNotEmpty() }
        val viewModel = WriteTagViewModel(FakeTagWriterSource(), repository, SavedStateHandle(mapOf("profileId" to profile.id)))

        viewModel.onWriteResult(TagWriteResult.Success)

        assertIs<WriteTagUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `a write failure surfaces the failure reason`() = runTest {
        val profile = Profile(name = "My Site", fields = NdefPayload.Text("hi"))
        repository.createProfile(profile)
        repository.profiles.first { it.isNotEmpty() }
        val viewModel = WriteTagViewModel(FakeTagWriterSource(), repository, SavedStateHandle(mapOf("profileId" to profile.id)))

        viewModel.onWriteResult(TagWriteResult.Failure("This tag is read-only"))

        val state = assertIs<WriteTagUiState.Failure>(viewModel.uiState.value)
        kotlin.test.assertEquals("This tag is read-only", state.reason)
    }

    @Test
    fun `dismissResult after a failure returns to waiting`() = runTest {
        val profile = Profile(name = "My Site", fields = NdefPayload.Text("hi"))
        repository.createProfile(profile)
        repository.profiles.first { it.isNotEmpty() }
        val viewModel = WriteTagViewModel(FakeTagWriterSource(), repository, SavedStateHandle(mapOf("profileId" to profile.id)))
        viewModel.onWriteResult(TagWriteResult.Failure("x"))

        viewModel.dismissResult()

        assertIs<WriteTagUiState.Waiting>(viewModel.uiState.value)
    }
}
