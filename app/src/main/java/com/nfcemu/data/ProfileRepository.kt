package com.nfcemu.data

import com.nfcemu.data.activity.RecentActivityRepository
import com.nfcemu.data.local.ProfileDataStore
import com.nfcemu.di.ApplicationScope
import com.nfcemu.domain.ActiveNdefSource
import com.nfcemu.ndefengine.AarConfig
import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ndefengine.NdefPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the profile library and which profile is active.
 * Implements [ActiveNdefSource] itself: the HCE service only ever reads
 * [currentNdefBytes], which is recomputed from [ProfileDataStore] and pushed through
 * automatically whenever the *active* profile's content changes - editing some other,
 * inactive profile updates [profiles] but leaves the cached NDEF bytes untouched
 * (see [distinctUntilChanged] below), so there's no needless re-encoding on the
 * service's behalf.
 *
 * Injecting this class anywhere (the [com.nfcemu.NfcEmuApplication] on process start)
 * is what starts the underlying DataStore collection - see the class kdoc on
 * [com.nfcemu.NfcEmuApplication] for why that eager instantiation matters.
 *
 * Every mutator below reads the current list via `dataStore.profiles.first()` - the
 * *direct*, authoritative Flow - rather than the [profiles] StateFlow exposed for UI
 * observation. [profiles] is kept in sync via [SharingStarted.Eagerly] on a
 * fire-and-forget scope, so two mutators called back-to-back (e.g. create then
 * immediately setActive) could otherwise race against that propagation and silently
 * read a stale, pre-write snapshot.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val dataStore: ProfileDataStore,
    private val recentActivityRepository: RecentActivityRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : ActiveNdefSource {

    val profiles: StateFlow<List<Profile>> = dataStore.profiles
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeProfileId: StateFlow<String?> = dataStore.activeProfileId
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val _currentNdefBytes = MutableStateFlow(ByteArray(0))
    override val currentNdefBytes: StateFlow<ByteArray> = _currentNdefBytes.asStateFlow()

    init {
        combine(profiles, activeProfileId) { list, activeId -> list.find { it.id == activeId } }
            .distinctUntilChanged()
            .onEach { activeProfile ->
                _currentNdefBytes.value = activeProfile?.let(::encode) ?: ByteArray(0)
            }
            .launchIn(scope)
    }

    private fun encode(profile: Profile): ByteArray =
        NdefMessageFactory.build(profile.fields, profile.aarPackageName?.let { AarConfig(it) })

    private suspend fun currentProfiles(): List<Profile> = dataStore.profiles.first()

    suspend fun createProfile(profile: Profile) {
        dataStore.saveProfiles(currentProfiles() + profile)
    }

    suspend fun updateProfile(profile: Profile) {
        dataStore.saveProfiles(currentProfiles().map { if (it.id == profile.id) profile else it })
    }

    suspend fun deleteProfile(id: String) {
        dataStore.saveProfiles(currentProfiles().filterNot { it.id == id })
        if (dataStore.activeProfileId.first() == id) {
            dataStore.saveActiveProfileId(null)
        }
    }

    suspend fun duplicateProfile(id: String): Profile? {
        val original = currentProfiles().find { it.id == id } ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            pinned = false,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null,
        )
        dataStore.saveProfiles(currentProfiles() + copy)
        return copy
    }

    suspend fun setActive(id: String) {
        val profiles = currentProfiles()
        val target = profiles.find { it.id == id } ?: return
        dataStore.saveProfiles(
            profiles.map { if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis()) else it },
        )
        dataStore.saveActiveProfileId(target.id)
        recentActivityRepository.record(target.name, target.fields.typeLabel())
    }

    /** Deactivates whichever profile is currently active - the card then emulates nothing. */
    suspend fun clearActive() {
        dataStore.saveActiveProfileId(null)
    }

    suspend fun togglePinned(id: String) {
        dataStore.saveProfiles(
            currentProfiles().map { if (it.id == id) it.copy(pinned = !it.pinned) else it },
        )
    }

    /**
     * Adds an imported profile to the library and returns it with its final id.
     * Never sets it active - the user must do that explicitly.
     */
    suspend fun importProfile(profile: Profile): Profile {
        val stored = profile.copy(id = UUID.randomUUID().toString(), pinned = false)
        dataStore.saveProfiles(currentProfiles() + stored)
        return stored
    }

    /**
     * Upserts the single reserved "My Profile" row ([Profile.MY_PROFILE_ID]). It's a real
     * [Profile] persisted alongside normal ones - so [setActive]/HCE emulation need no
     * special-casing - but is filtered out of every user-facing profile list (see
     * [com.nfcemu.ui.home.HomeViewModel]/[com.nfcemu.ui.profilelist.ProfileListViewModel]).
     */
    suspend fun saveMyProfile(vcard: NdefPayload.VCard) {
        val profiles = currentProfiles()
        val existing = profiles.find { it.id == Profile.MY_PROFILE_ID }
        val row = Profile(
            id = Profile.MY_PROFILE_ID,
            name = vcard.name?.takeIf { it.isNotBlank() } ?: "My Profile",
            fields = vcard,
            pinned = false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            lastUsedAt = existing?.lastUsedAt,
        )
        dataStore.saveProfiles(
            if (existing != null) profiles.map { if (it.id == Profile.MY_PROFILE_ID) row else it } else profiles + row,
        )
    }
}
