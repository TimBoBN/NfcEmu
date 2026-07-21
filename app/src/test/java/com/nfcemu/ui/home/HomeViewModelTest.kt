package com.nfcemu.ui.home

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.data.local.ProfileDataStore
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.nfc.NfcHardwareState
import com.nfcemu.nfc.NfcStateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

private class FakeNfcStateSource(initial: NfcHardwareState = NfcHardwareState.ENABLED) : NfcStateSource {
    private val flow = MutableStateFlow(initial)
    override val state: Flow<NfcHardwareState> = flow
}

/**
 * [HomeViewModel] is exercised against a real temp-file-backed [ProfileRepository] (same
 * approach as [com.nfcemu.data.ProfileRepositoryTest]) plus a [FakeNfcStateSource], so
 * quick-select composition (pinned + recently used) is tested end-to-end rather than
 * mocking the repository's Flow shapes.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var tempDir: File
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: ProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempDir = File.createTempFile("nfcemu-home-vm-test", "").apply { delete(); mkdirs() }
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
    fun `quick select includes pinned and recently used profiles, both excluding the active one`() = runTest {
        val pinned = Profile(name = "Pinned", fields = NdefPayload.Text("p"), pinned = true)
        val recentlyUsed = Profile(name = "RecentlyUsed", fields = NdefPayload.Text("r"))
        val active = Profile(name = "Active", fields = NdefPayload.Text("a"))
        repository.createProfile(pinned)
        repository.createProfile(recentlyUsed)
        repository.createProfile(active)
        repository.setActive(recentlyUsed.id) // marks lastUsedAt
        repository.setActive(active.id) // now active; recentlyUsed remains "recently used" but inactive
        repository.profiles.first { it.size == 3 }

        val viewModel = HomeViewModel(repository, FakeNfcStateSource())
        val state = viewModel.uiState.first { it.quickSelectProfiles.size == 2 }

        assertEquals(setOf("Pinned", "RecentlyUsed"), state.quickSelectProfiles.map { it.name }.toSet())
        assertTrue(state.quickSelectProfiles.none { it.id == active.id })
    }

    @Test
    fun `the active profile itself is excluded from the recent (non-pinned) quick-select bucket`() = runTest {
        val active = Profile(name = "Active", fields = NdefPayload.Text("a"))
        val other = Profile(name = "Other", fields = NdefPayload.Text("o"))
        repository.createProfile(active)
        repository.createProfile(other)
        repository.setActive(active.id)
        repository.setActive(other.id) // "other" was used, then we switch back
        repository.setActive(active.id)
        repository.activeProfileId.first { it == active.id }

        val viewModel = HomeViewModel(repository, FakeNfcStateSource())
        val state = viewModel.uiState.first { it.activeProfile != null }

        assertTrue(state.quickSelectProfiles.none { it.id == active.id })
        assertEquals(active.id, state.activeProfile?.id)
    }

    @Test
    fun `selecting a profile from quick-select activates it`() = runTest {
        val profile = Profile(name = "Target", fields = NdefPayload.Text("t"))
        repository.createProfile(profile)
        val viewModel = HomeViewModel(repository, FakeNfcStateSource())

        viewModel.selectProfile(profile.id)

        assertEquals(profile.id, repository.activeProfileId.first { it == profile.id })
    }

    @Test
    fun `nfc hardware state is surfaced from the nfc state source`() = runTest {
        val viewModel = HomeViewModel(repository, FakeNfcStateSource(NfcHardwareState.DISABLED))
        val state = viewModel.uiState.first { it.nfcState == NfcHardwareState.DISABLED }
        assertEquals(NfcHardwareState.DISABLED, state.nfcState)
    }
}
