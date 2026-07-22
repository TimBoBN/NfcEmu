package com.nfcemu.lock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.local.SettingsDataStore
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DefaultAppLockStateTest {

    private lateinit var tempDir: File
    private lateinit var scope: CoroutineScope
    private lateinit var settingsDataStore: SettingsDataStore

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tempDir = File.createTempFile("nfcemu-app-lock-test", "").apply { delete(); mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { File(tempDir, "settings.preferences_pb") })
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        settingsDataStore = SettingsDataStore(dataStore)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun `starts null then settles to false when the setting is off`() = runTest {
        val state = DefaultAppLockState(settingsDataStore, scope)
        assertEquals(false, state.isLocked.first { it != null })
    }

    @Test
    fun `settles to true when the setting is on`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val state = DefaultAppLockState(settingsDataStore, scope)
        assertEquals(true, state.isLocked.first { it != null })
    }

    @Test
    fun `onAppBackgrounded only locks when the setting is enabled`() = runTest {
        val state = DefaultAppLockState(settingsDataStore, scope)
        state.isLocked.first { it != null }

        state.onAppBackgrounded()

        assertEquals(false, state.isLocked.value)
    }

    @Test
    fun `onAppBackgrounded locks when the setting is enabled`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val state = DefaultAppLockState(settingsDataStore, scope)
        state.isLocked.first { it != null }

        state.onAppBackgrounded()

        assertEquals(true, state.isLocked.value)
    }

    @Test
    fun `onAppBackgrounded is idempotent when already locked`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val state = DefaultAppLockState(settingsDataStore, scope)
        state.isLocked.first { it != null }

        state.onAppBackgrounded()
        state.onAppBackgrounded()

        assertEquals(true, state.isLocked.value)
    }

    @Test
    fun `unlock clears the lock`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val state = DefaultAppLockState(settingsDataStore, scope)
        state.isLocked.first { it != null }
        state.onAppBackgrounded()

        state.unlock()

        assertEquals(false, state.isLocked.value)
    }

    @Test
    fun `disableAndUnlock clears the lock and persists the setting off`() = runTest {
        settingsDataStore.setRequireBiometricUnlock(true)
        val state = DefaultAppLockState(settingsDataStore, scope)
        state.isLocked.first { it != null }
        state.onAppBackgrounded()

        state.disableAndUnlock()

        assertEquals(false, state.isLocked.value)
        // disableAndUnlock persists via a launch{} on `scope` - a real (non-test) dispatcher -
        // so this must wait for the eventual write rather than assume it already landed.
        assertEquals(false, settingsDataStore.requireBiometricUnlock.first { !it })
    }
}
