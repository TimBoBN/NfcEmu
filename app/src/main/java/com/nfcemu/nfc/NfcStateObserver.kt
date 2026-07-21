package com.nfcemu.nfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NfcHardwareState { NOT_SUPPORTED, DISABLED, ENABLED }

/** Narrow, mockable contract so ViewModels don't depend on the concrete Android-backed observer directly. */
interface NfcStateSource {
    val state: Flow<NfcHardwareState>
}

/** Reacts to NFC being toggled at runtime via [NfcAdapter.ACTION_ADAPTER_STATE_CHANGED]. */
@Singleton
class NfcStateObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) : NfcStateSource {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    override val state: Flow<NfcHardwareState> = callbackFlow {
        if (adapter == null) {
            trySend(NfcHardwareState.NOT_SUPPORTED)
            awaitClose { }
            return@callbackFlow
        }

        trySend(if (adapter.isEnabled) NfcHardwareState.ENABLED else NfcHardwareState.DISABLED)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(if (adapter.isEnabled) NfcHardwareState.ENABLED else NfcHardwareState.DISABLED)
            }
        }
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        awaitClose { context.unregisterReceiver(receiver) }
    }
}
