package com.nfcemu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.nfcemu.R

/**
 * Inter, vendored as a single variable font file (`res/font/inter_variable.ttf`, OFL-licensed -
 * see `assets/licenses/inter_OFL.txt`) rather than Google Fonts' downloadable-font provider, to
 * preserve the app's "fully offline, no network permission" property. Only weights 400/500 are
 * used anywhere in the design - the system never goes past medium.
 */
@OptIn(ExperimentalTextApi::class)
private val Inter = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_variable, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

private val defaults = Typography()

/** Headings never go bolder than medium (500) - hierarchy is size/space, not weight. */
private fun TextStyle.asHeading() = copy(fontFamily = Inter, fontWeight = FontWeight.Medium, letterSpacing = (-0.015).em)

private fun TextStyle.asBody() = copy(fontFamily = Inter)

val NocturneTypography = Typography(
    displayLarge = defaults.displayLarge.asHeading(),
    displayMedium = defaults.displayMedium.asHeading(),
    displaySmall = defaults.displaySmall.asHeading(),
    headlineLarge = defaults.headlineLarge.asHeading(),
    headlineMedium = defaults.headlineMedium.asHeading(),
    headlineSmall = defaults.headlineSmall.asHeading(),
    titleLarge = defaults.titleLarge.asHeading(),
    titleMedium = defaults.titleMedium.asHeading(),
    titleSmall = defaults.titleSmall.asHeading(),
    bodyLarge = defaults.bodyLarge.asBody(),
    bodyMedium = defaults.bodyMedium.asBody(),
    bodySmall = defaults.bodySmall.asBody(),
    labelLarge = defaults.labelLarge.asBody(),
    labelMedium = defaults.labelMedium.asBody(),
    labelSmall = defaults.labelSmall.asBody(),
)
