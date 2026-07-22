package com.nfcemu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The circular type-icon badge used throughout (Home's carousel, Profile List rows, Library
 * rows, Transmit's floating card): [MaterialTheme]'s `primaryContainer`/`onPrimaryContainer`
 * pair as background/foreground, wrapping either an icon or the Text type's "Aa" glyph.
 */
@Composable
fun TypeIconBadge(glyph: TypeGlyph, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (glyph) {
            is TypeGlyph.Icon -> Icon(
                imageVector = glyph.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f),
            )
            TypeGlyph.Text -> Text(
                "Aa",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
