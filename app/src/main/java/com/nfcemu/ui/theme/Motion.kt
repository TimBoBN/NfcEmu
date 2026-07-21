package com.nfcemu.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Single source of truth for motion timing so every screen transition and
 * micro-interaction in the app feels consistent instead of each screen picking
 * its own duration/easing ad hoc.
 */
object Motion {
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 300

    val emphasizedEasing = FastOutSlowInEasing

    fun <T> standard(durationMillis: Int = DURATION_MEDIUM) = tween<T>(
        durationMillis = durationMillis,
        easing = emphasizedEasing,
    )
}
