package com.nfcemu.ui.home

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.Profile
import com.nfcemu.data.ProfileRepository
import com.nfcemu.data.activity.RecentActivityDataStore
import com.nfcemu.data.activity.RecentActivityRepository
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

private class FakeNfcStateSource(initial: NfcHardwareState = NfcHardwareState.ENABLED) : NfcStateSource {
    private val flow = MutableStateFlow(initial)
    override val state: Flow<NfcHardwareState> = flow
}

/**
 * [HomeViewModel] is exercised against a real temp-file-backed [ProfileRepository] (same
 * approach as [com.nfcemu.data.ProfileRepositoryTest]) plus a [FakeNfcStateSource], so
 * quick-select composition (manual [Profile.quickSelectOrder] curation) is tested end-to-end
 * rather than mocking the repository's Flow shapes.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var repositoryScope: CoroutineScope
    private lateinit var repository: ProfileRepository
    private lateinit var recentActivityRepository: RecentActivityRepository

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        tempDir = File.createTempFile("nfcemu-home-vm-test", "").apply { delete(); mkdirs() }
        // Built on the same dispatcher installed as Main above (not Dispatchers.Default), so
        // runTest's virtual scheduler can see and deterministically drive this scope's work -
        // otherwise repository/DataStore internals run on a real, untracked dispatcher that
        // can race against runTest's own end-of-test bookkeeping under load.
        repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
        val dataStore = PreferenceDataStoreFactory.create(scope = repositoryScope, produceFile = { File(tempDir, "profiles.preferences_pb") })
        val activityDataStore = PreferenceDataStoreFactory.create(scope = repositoryScope, produceFile = { File(tempDir, "recent_activity.preferences_pb") })
        recentActivityRepository = RecentActivityRepository(RecentActivityDataStore(activityDataStore), repositoryScope)
        repository = ProfileRepository(ProfileDataStore(dataStore), recentActivityRepository, repositoryScope)
    }

    @AfterTest
    fun tearDown() {
        repositoryScope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `quick select only includes profiles explicitly added, in the order they were added`() = runTest {
        val first = Profile(name = "First", fields = NdefPayload.Text("f"))
        val second = Profile(name = "Second", fields = NdefPayload.Text("s"))
        val notSelected = Profile(name = "NotSelected", fields = NdefPayload.Text("n"))
        repository.createProfile(first)
        repository.createProfile(second)
        repository.createProfile(notSelected)
        repository.profiles.first { it.size == 3 }

        repository.addToQuickSelect(second.id)
        repository.addToQuickSelect(first.id)

        val viewModel = HomeViewModel(repository, FakeNfcStateSource(), recentActivityRepository)
        val state = viewModel.uiState.first { it.quickSelectProfiles.size == 2 }

        assertEquals(listOf(second.id, first.id), state.quickSelectProfiles.map { it.id })
    }

    @Test
    fun `quick select includes the active profile if it was explicitly added, and reflects reordering`() = runTest {
        val active = Profile(name = "Active", fields = NdefPayload.Text("a"))
        val other = Profile(name = "Other", fields = NdefPayload.Text("o"))
        repository.createProfile(active)
        repository.createProfile(other)
        repository.addToQuickSelect(active.id)
        repository.addToQuickSelect(other.id)
        repository.setActive(active.id)
        repository.reorderQuickSelect(listOf(other.id, active.id))

        val viewModel = HomeViewModel(repository, FakeNfcStateSource(), recentActivityRepository)
        val state = viewModel.uiState.first { it.activeProfile != null }

        assertEquals(listOf(other.id, active.id), state.quickSelectProfiles.map { it.id })
        assertEquals(active.id, state.activeProfile?.id)
    }

    @Test
    fun `selecting a profile from quick-select activates it`() = runTest {
        val profile = Profile(name = "Target", fields = NdefPayload.Text("t"))
        repository.createProfile(profile)
        val viewModel = HomeViewModel(repository, FakeNfcStateSource(), recentActivityRepository)

        viewModel.selectProfile(profile.id)

        assertEquals(profile.id, repository.activeProfileId.first { it == profile.id })
    }

    @Test
    fun `nfc hardware state is surfaced from the nfc state source`() = runTest {
        val viewModel = HomeViewModel(repository, FakeNfcStateSource(NfcHardwareState.DISABLED), recentActivityRepository)
        val state = viewModel.uiState.first { it.nfcState == NfcHardwareState.DISABLED }
        assertEquals(NfcHardwareState.DISABLED, state.nfcState)
    }
}
