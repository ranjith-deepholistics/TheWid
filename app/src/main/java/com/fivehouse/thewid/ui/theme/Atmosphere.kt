package com.fivehouse.thewid.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** The two colours a glossy shape's radial gradient passes through after its white centre. */
@Immutable
data class GlossTint(val light: Color, val deep: Color) {
    companion object {
        val Accent =
            GlossTint(
                light = lerp(com.fivehouse.thewid.ui.theme.Accent, Color.White, 0.5f),
                deep = com.fivehouse.thewid.ui.theme.Accent
            )

        /** The gold of the Time widget's day shape. Sampled from mockup 17; the spec has no token for it. */
        val Sun = GlossTint(light = Color(0xFFFFF1C9), deep = Color(0xFFEFC45D))
    }
}

/** Screen backgrounds. [stops] run top to bottom. */
enum class Atmosphere(val stops: List<Color>, val isDark: Boolean = false) {
    CoolMist(listOf(Color(0xFFB7CBE2), Color(0xFFCFDDEC), Color(0xFFEEF3F8))),
    DuskBloom(listOf(Color(0xFFF8F6F7), Color(0xFFF6ECF0), Color(0xFFF0CFDD), Color(0xFFEBB3CA))),
    Moss(listOf(Color(0xFF1A261E), Color(0xFF22362A), Color(0xFF3A5A43)), isDark = true),
    Lilac(listOf(Color(0xFFF6F4F9), Color(0xFFEAE3F4), Color(0xFFD9CDEE))),
    Mint(listOf(Color(0xFFF4F8F6), Color(0xFFE6F2EC), Color(0xFFC9E4D6))),
    Stone(listOf(Color(0xFFE9E3DA), Color(0xFFF2EEE8), Color(0xFFFAF8F5))),
    Peach(listOf(Color(0xFFFBF6F2), Color(0xFFF8EADF), Color(0xFFF2CDB4))),
    Night(listOf(Color(0xFF121A30), Color(0xFF1A2645), Color(0xFF2B3A66)), isDark = true);

    val background: Brush = Brush.verticalGradient(stops)

    /** Glossy tints derived from the atmosphere's own stops, per the spec's "tint of the atmosphere". */
    val gloss: GlossTint = glossFor(stops, isDark)
}

private fun glossFor(stops: List<Color>, isDark: Boolean): GlossTint {
    val byLuminance = stops.sortedBy { it.luminance() }
    val darkest = byLuminance.first()
    val brightest = byLuminance.last()
    return if (isDark) {
        GlossTint(light = lerp(brightest, Color.White, 0.55f), deep = brightest)
    } else {
        GlossTint(light = byLuminance[byLuminance.size / 2], deep = lerp(darkest, Ink, 0.18f))
    }
}
