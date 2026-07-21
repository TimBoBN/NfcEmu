package com.nfcemu.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.nfcemu.domain.ActiveNdefSource
import com.nfcemu.ndefengine.hce.Type4TagApduProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Thin Android adapter around [Type4TagApduProcessor]: all Type 4 Tag / ISO 7816-4
 * protocol logic lives in the Android-free ndefengine module so it can be unit
 * tested without an emulator. This class only wires Android lifecycle callbacks and
 * the active-profile [ActiveNdefSource] flow to that processor.
 *
 * Constraint: [processCommandApdu] is called on a binder thread and must return
 * quickly - it must never block on I/O. The current NDEF bytes are only ever cached
 * state pushed in via [ActiveNdefSource]'s Flow (updated by the repository whenever
 * the active profile changes), never read from disk here.
 */
@AndroidEntryPoint
class NfcEmuHostApduService : HostApduService() {

    @Inject
    lateinit var activeNdefSource: ActiveNdefSource

    private val processor = Type4TagApduProcessor()
    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        serviceScope = scope
        activeNdefSource.currentNdefBytes
            .onEach { bytes -> processor.updateNdefMessage(bytes) }
            .launchIn(scope)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        return processor.process(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        val reasonText = when (reason) {
            DEACTIVATION_LINK_LOSS -> "LINK_LOSS (field lost)"
            DEACTIVATION_DESELECTED -> "DESELECTED (reader moved to another AID)"
            else -> "UNKNOWN($reason)"
        }
        Log.i(TAG, "Card emulation deactivated: $reasonText")
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NfcEmuHostApduService"
    }
}
