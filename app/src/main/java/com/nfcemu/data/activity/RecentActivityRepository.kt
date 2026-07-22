package com.nfcemu.data.activity

import com.nfcemu.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentActivityRepository @Inject constructor(
    private val dataStore: RecentActivityDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val recent: StateFlow<List<RecentActivityEntry>> = dataStore.entries
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Reads the DataStore Flow directly (not [recent]) - see ProfileRepository's kdoc for why. */
    suspend fun record(name: String, typeLabel: String) {
        val updated = (listOf(RecentActivityEntry(name = name, typeLabel = typeLabel)) + dataStore.entries.first())
            .take(MAX_ENTRIES)
        dataStore.save(updated)
    }

    private companion object {
        const val MAX_ENTRIES = 5
    }
}
