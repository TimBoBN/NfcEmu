package com.nfcemu.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.local.SettingsDataStore
import com.nfcemu.lock.BiometricAvailability
import com.nfcemu.lock.BiometricAvailabilitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeBiometricAvailabilitySource(private val availability: BiometricAvailability) : BiometricAvailabilitySource {
    override fun currentAvailability(): BiometricAvailability = availability
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: CoroutineScope
    private lateinit var settingsDataStore: SettingsDataStore

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        tempDir = File.createTempFile("nfcemu-settings-vm-test", "").apply { delete(); mkdirs() }
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        // Passing our own `scope` (cancelled in tearDown) instead of letting the factory
        // default to its own uncancelled Dispatchers.IO scope - otherwise DataStore's
        // internal write-actor coroutine leaks for the rest of the test JVM's lifetime,
        // one per test, which piles up across the whole suite and can starve
        // Dispatchers.IO/Default badly enough on a constrained CI runner to hang later,
        // unrelated tests for a full minute (UncompletedCoroutinesError).
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(tempDir, "settings.preferences_pb") })
        settingsDataStore = SettingsDataStore(dataStore)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects the persisted flag`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.AVAILABLE))

        val state = viewModel.uiState.first { it.requireBiometricUnlock }

        assertEquals(true, state.requireBiometricUnlock)
        assertEquals(BiometricAvailability.AVAILABLE, state.biometricAvailability)
    }

    @Test
    fun `enabling persists when biometric is available`() = runTest {
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.AVAILABLE))

        viewModel.setRequireBiometricUnlock(true)

        // Persists via viewModelScope.launch{} - DataStore's write completes asynchronously
        // on its own dispatcher, so this must wait for the eventual value, not assume it landed.
        assertEquals(true, settingsDataStore.requireBiometricUnlock.first { it })

        // Drains any trailing continuation of that launch{} still pending in the shared
        // test scheduler (viewModelScope shares it via Dispatchers.setMain above) - without
        // this, runTest can end while that coroutine hasn't formally completed yet, even
        // though the write it performed has already landed and been observed above.
        advanceUntilIdle()
    }

    @Test
    fun `enabling is a no-op when no biometric or credential is configured`() = runTest {
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.NOT_CONFIGURED))

        viewModel.setRequireBiometricUnlock(true)
        advanceUntilIdle()

        assertEquals(false, settingsDataStore.requireBiometricUnlock.first())
    }

    @Test
    fun `disabling always persists regardless of biometric availability`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.UNSUPPORTED))

        viewModel.setRequireBiometricUnlock(false)

        assertEquals(false, settingsDataStore.requireBiometricUnlock.first { !it })
        advanceUntilIdle()
    }
}
