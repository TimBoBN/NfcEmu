package com.nfcemu.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.local.SettingsDataStore
import com.nfcemu.lock.BiometricAvailability
import com.nfcemu.lock.BiometricAvailabilitySource
import kotlinx.coroutines.Dispatchers
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

private class FakeBiometricAvailabilitySource(private val availability: BiometricAvailability) : BiometricAvailabilitySource {
    override fun currentAvailability(): BiometricAvailability = availability
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var tempDir: File
    private lateinit var settingsDataStore: SettingsDataStore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempDir = File.createTempFile("nfcemu-settings-vm-test", "").apply { delete(); mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { File(tempDir, "settings.preferences_pb") })
        settingsDataStore = SettingsDataStore(dataStore)
    }

    @AfterTest
    fun tearDown() {
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
    }

    @Test
    fun `enabling is a no-op when no biometric or credential is configured`() = runTest {
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.NOT_CONFIGURED))

        viewModel.setRequireBiometricUnlock(true)

        assertEquals(false, settingsDataStore.requireBiometricUnlock.first())
    }

    @Test
    fun `disabling always persists regardless of biometric availability`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val viewModel = SettingsViewModel(settingsDataStore, FakeBiometricAvailabilitySource(BiometricAvailability.UNSUPPORTED))

        viewModel.setRequireBiometricUnlock(false)

        assertEquals(false, settingsDataStore.requireBiometricUnlock.first { !it })
    }
}
