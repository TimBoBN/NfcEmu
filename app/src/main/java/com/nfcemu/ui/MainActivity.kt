package com.nfcemu.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nfcemu.ui.navigation.NfcEmuNavGraph
import com.nfcemu.ui.theme.NfcEmuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NfcEmuTheme {
                NfcEmuNavGraph()
            }
        }
    }

    /**
     * NfcEmu is a card-emulation app: while it's open, we never want this phone
     * acting as an NFC *reader* too. Without this, bringing it close to another
     * HCE-capable phone can make Android's own default tag-dispatch kick in on
     * our side and show a generic "empty tag" notification for the other device.
     * Claiming foreground dispatch for tag-discovery intents (and then simply
     * ignoring them in [onNewIntent]) suppresses that without touching our own
     * HostApduService, which answers reader requests independently of this.
     */
    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
        )
        adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Intentionally a no-op: this exists purely to absorb tag-discovery intents
        // (see onResume kdoc) while the app is in the foreground, not to read tags.
    }
}
