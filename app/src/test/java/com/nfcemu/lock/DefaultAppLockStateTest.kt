package com.nfcemu.lock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nfcemu.data.local.SettingsDataStore
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
class DefaultAppLockStateTest {

    private lateinit var tempDir: File
    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: CoroutineScope
    private lateinit var settingsDataStore: SettingsDataStore

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        tempDir = File.createTempFile("nfcemu-app-lock-test", "").apply { delete(); mkdirs() }
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
