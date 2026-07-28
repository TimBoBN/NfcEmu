package com.nfcemu.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * Single source of truth for motion timing so every screen transition and
 * micro-interaction in the app feels consistent instead of each screen picking
 * its own duration/easing ad hoc.
 *
 * The curves below are the Material 3 motion spec's published cubic-bezier control points -
 * M3 doesn't expose them as public API (they're internal to `androidx.compose.material3`), and
 * they're a visibly different, more natural shape than Material 2's [androidx.compose.animation.core.FastOutSlowInEasing]
 * this previously aliased for everything, symmetric curve or not.
 */
object Motion {
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 300

    /** M3 "Standard" easing - default for simple two-way effects (fade, expand/shrink, color, scale). */
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** M3 "Emphasized decelerate" - content settling into view; pairs with [emphasizedAccelerate]. */
    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** M3 "Emphasized accelerate" - content leaving view; pairs with [emphasizedDecelerate]. */
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> standard(durationMillis: Int = DURATION_MEDIUM) = tween<T>(
        durationMillis = durationMillis,
        easing = standardEasing,
    )
}
