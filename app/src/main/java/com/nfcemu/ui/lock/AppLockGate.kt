package com.nfcemu.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Wraps the app's nav graph: renders nothing while the persisted lock setting is still being
 * read at process start ([LockGateViewModel.isLocked] is `null`), [LockScreen] while locked,
 * or [content] once unlocked/never-locked. See [com.nfcemu.lock.AppLockState]'s kdoc for what
 * this does and does not protect.
 */
@Composable
fun AppLockGate(viewModel: LockGateViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val locked by viewModel.isLocked.collectAsState()
    when (locked) {
        null -> Unit
        true -> LockScreen(viewModel = viewModel)
        false -> content()
    }
}
