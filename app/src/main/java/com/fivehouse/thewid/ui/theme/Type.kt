package com.fivehouse.thewid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.fivehouse.thewid.R

private fun variableFont(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val RobotoFlex = FontFamily(
    variableFont(R.font.roboto_flex, 200),
    variableFont(R.font.roboto_flex, 300),
    variableFont(R.font.roboto_flex, 400),
    variableFont(R.font.roboto_flex, 500)
)

val Doto = FontFamily(
    variableFont(R.font.doto, 700),
    variableFont(R.font.doto, 900)
)

private fun flex(size: TextUnit, lineHeight: TextUnit, weight: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = RobotoFlex,
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking
)

val ShapesTypography = Typography(
    displayLarge = flex(57.sp, 64.sp, 200),
    displayMedium = flex(45.sp, 52.sp, 200),
    displaySmall = flex(36.sp, 44.sp, 200),
    headlineLarge = flex(32.sp, 40.sp, 300),
    headlineMedium = flex(28.sp, 36.sp, 300),
    headlineSmall = flex(24.sp, 32.sp, 300),
    titleLarge = flex(22.sp, 28.sp, 400),
    titleMedium = flex(16.sp, 24.sp, 500),
    titleSmall = flex(14.sp, 20.sp, 500),
    bodyLarge = flex(16.sp, 24.sp, 400),
    bodyMedium = flex(14.sp, 20.sp, 400),
    bodySmall = flex(12.sp, 16.sp, 400),
    labelLarge = flex(14.sp, 20.sp, 500),
    labelMedium = flex(12.sp, 16.sp, 500),
    labelSmall = flex(11.sp, 16.sp, 500, tracking = 1.5.sp)
)

/** Doto dot-matrix styles for every hero number and value. */
object Numerals {
    private fun doto(size: TextUnit, weight: Int) = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight(weight),
        fontSize = size,
        lineHeight = size
    )

    val Hero = doto(72.sp, 900)
    val Large = doto(48.sp, 900)
    val Medium = doto(28.sp, 900)
    val Small = doto(18.sp, 700)
}
