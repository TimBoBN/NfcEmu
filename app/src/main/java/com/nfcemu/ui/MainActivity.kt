package com.nfcemu.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nfcemu.data.ProfileRepository
import com.nfcemu.shortcuts.ProfileShortcutUpdater
import com.nfcemu.ui.lock.AppLockGate
import com.nfcemu.ui.navigation.NfcEmuNavGraph
import com.nfcemu.ui.theme.NfcEmuTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Extends [FragmentActivity] (not just `ComponentActivity`) because [androidx.biometric.BiometricPrompt]'s constructor requires one. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    private var pendingSharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleActivateProfileIntent(intent)
        handleShareIntent(intent)
        setContent {
            NfcEmuTheme {
                AppLockGate {
                    NfcEmuNavGraph(
                        pendingSharedText = pendingSharedText,
                        onSharedTextConsumed = { pendingSharedText = null },
                    )
                }
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

    /**
     * Handles both a warm restart (this) and the initial launch (onCreate's `intent`).
     * Tag-discovery intents (see onResume kdoc) fall through unhandled here - they're
     * absorbed simply by not being acted on, not by an explicit branch - so that
     * behavior is unaffected by the shortcut-activation branch added below.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleActivateProfileIntent(intent)
        handleShareIntent(intent)
    }

    /** Target of a launcher shortcut tap (see [ProfileShortcutUpdater]): activates the profile it names. */
    private fun handleActivateProfileIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val profileId = intent.getStringExtra(ProfileShortcutUpdater.EXTRA_ACTIVATE_PROFILE_ID) ?: return
        lifecycleScope.launch { profileRepository.setActive(profileId) }
    }

    /**
     * Target of the Android share sheet ("Share" -> NfcEmu on a link or phone number from
     * another app, see the `ACTION_SEND` filter in AndroidManifest.xml). Doesn't activate
     * anything directly - just hands the raw text to [NfcEmuNavGraph], which routes to
     * `sharePreview` so the user confirms/corrects it before anything broadcasts.
     */
    private fun handleShareIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        pendingSharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    }
}
