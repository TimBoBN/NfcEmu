package com.nfcemu.shortcuts

import com.nfcemu.data.Profile
import com.nfcemu.ndefengine.NdefPayload
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [ProfileShortcutUpdater.selectShortcutProfiles] is a pure function extracted from the
 * Context-dependent shortcut publishing so it can be tested directly, the same way
 * [com.nfcemu.ui.home.HomeViewModel]'s quick-select composition is exercised end-to-end
 * via [com.nfcemu.ui.home.HomeViewModelTest].
 */
class ProfileShortcutUpdaterTest {

    private fun profile(
        name: String,
        pinned: Boolean = false,
        createdAt: Long = 0L,
        lastUsedAt: Long? = null,
    ) = Profile(name = name, fields = NdefPayload.Text(name), pinned = pinned, createdAt = createdAt, lastUsedAt = lastUsedAt)

    @Test
    fun `only pinned profiles become shortcuts`() {
        val pinned = profile("Pinned", pinned = true)
        val unpinned = profile("Unpinned")

        val result = ProfileShortcutUpdater.selectShortcutProfiles(listOf(pinned, unpinned))

        assertEquals(listOf("Pinned"), result.map { it.name })
    }

    @Test
    fun `pinned profiles are ordered by most recently used, falling back to creation time`() {
        val oldest = profile("Oldest", pinned = true, createdAt = 1L)
        val usedRecently = profile("UsedRecently", pinned = true, createdAt = 2L, lastUsedAt = 100L)
        val neverUsed = profile("NeverUsed", pinned = true, createdAt = 3L)

        val result = ProfileShortcutUpdater.selectShortcutProfiles(listOf(oldest, usedRecently, neverUsed))

        assertEquals(listOf("UsedRecently", "NeverUsed", "Oldest"), result.map { it.name })
    }

    @Test
    fun `pinned profiles beyond the max are dropped, keeping the most recently used ones`() {
        val profiles = (1..6).map { profile("Pinned$it", pinned = true, createdAt = it.toLong(), lastUsedAt = it.toLong()) }

        val result = ProfileShortcutUpdater.selectShortcutProfiles(profiles)

        assertEquals(ProfileShortcutUpdater.MAX_SHORTCUTS, result.size)
        assertEquals(listOf("Pinned6", "Pinned5", "Pinned4", "Pinned3"), result.map { it.name })
    }
}
