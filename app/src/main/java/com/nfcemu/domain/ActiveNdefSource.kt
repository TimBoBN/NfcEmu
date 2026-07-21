package com.nfcemu.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only view of the currently emulated NDEF bytes, consumed only by
 * [com.nfcemu.hce.NfcEmuHostApduService]. Implemented by `ProfileRepository`
 * (data layer), which owns recomputing these bytes whenever the active profile
 * changes. Kept as a narrow interface so the HCE service depends only on this
 * contract, not on the full repository/DataStore surface.
 */
interface ActiveNdefSource {
    val currentNdefBytes: StateFlow<ByteArray>
}
