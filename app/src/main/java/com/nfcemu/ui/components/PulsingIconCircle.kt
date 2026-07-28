package com.nfcemu.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nfcemu.ui.theme.Motion

/**
 * A static bordered circle with a centered icon, plus a single ring expanding outward and
 * fading as it grows - mirrors the mockup's `@keyframes nfcPulse` (a growing, fading
 * `box-shadow` ring around a fixed circle), used by Onboarding and Scan Tag. Eased rather than
 * linear so the ring decelerates as it grows, like a real pulse, instead of expanding at a
 * constant mechanical speed.
 */
@Composable
fun PulsingIconCircle(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = Motion.standardEasing), repeatMode = RepeatMode.Restart),
        label = "pulse-ring",
    )
    val maxRingGrowth = 26.dp

    Box(modifier = modifier.size(size + maxRingGrowth * 2), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size + maxRingGrowth * 2 * ringProgress)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f * (1f - ringProgress)), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.45f))
        }
    }
}
