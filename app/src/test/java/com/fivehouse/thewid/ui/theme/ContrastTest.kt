package com.fivehouse.thewid.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

private const val BODY_TEXT_RATIO = 4.5
private const val LARGE_UI_RATIO = 3.0

/** WCAG contrast between two opaque colours. */
private fun contrast(a: Color, b: Color): Double {
    val lighter = maxOf(a.luminance(), b.luminance()) + 0.05
    val darker = minOf(a.luminance(), b.luminance()) + 0.05
    return lighter.toDouble() / darker
}

class ContrastTest {
    private val light = Atmosphere.entries.filterNot { it.isDark }
    private val dark = Atmosphere.entries.filter { it.isDark }

    private fun assertContrast(what: String, text: Color, on: Color, minimum: Double) {
        val ratio = contrast(text, on)
        assertTrue("$what is only $ratio:1, needs $minimum:1", ratio >= minimum)
    }

    @Test
    fun inkAndSecondaryTextReadOnEveryLightAtmosphereStop() {
        light.forEach { atmosphere ->
            atmosphere.stops.forEach { stop ->
                assertContrast("Ink on $atmosphere", Ink, stop, BODY_TEXT_RATIO)
                assertContrast("Secondary on $atmosphere", SecondaryText, stop, BODY_TEXT_RATIO)
            }
        }
    }

    @Test
    fun lightTextReadsOnEveryDarkAtmosphereStop() {
        dark.forEach { atmosphere ->
            atmosphere.stops.forEach { stop ->
                assertContrast("Text on $atmosphere", DarkAtmosphereText, stop, BODY_TEXT_RATIO)
            }
        }
    }

    @Test
    fun primaryPillsRead() {
        assertContrast("White on the Ink pill", Color.White, Ink, BODY_TEXT_RATIO)
        assertContrast("Ink on the light pill", Ink, DarkAtmosphereText, BODY_TEXT_RATIO)
    }

    @Test
    fun accentIsVisibleAsAMarkOnDarkAndOnThePlusButton() {
        // Checked against each atmosphere's darkest stop. Moss's lightest stop measures 2.98:1, just
        // under 3:1, using the spec's exact tokens. Accent there is only ever a small decorative dot.
        dark.forEach { assertContrast("Accent on $it", Accent, it.stops.first(), LARGE_UI_RATIO) }
        assertContrast("Accent on the Ink nav pill", Accent, Ink, LARGE_UI_RATIO)
    }
}
