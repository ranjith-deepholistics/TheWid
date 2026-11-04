package com.fivehouse.thewid.data.settings

import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.StatusWidgetKind
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesTest {
    @Test
    fun withNoChoiceEachScreenKeepsItsDesignedAtmosphere() {
        val prefs = AppPreferences(theme = ThemeChoice.Light)
        assertEquals(Atmosphere.Mint, prefs.atmosphereFor(Atmosphere.Mint, systemDark = true))
    }

    @Test
    fun chosenAtmosphereReplacesTheScreensOwn() {
        val prefs = AppPreferences(theme = ThemeChoice.Light, atmosphere = Atmosphere.DuskBloom)
        assertEquals(Atmosphere.DuskBloom, prefs.atmosphereFor(Atmosphere.Mint, systemDark = false))
    }

    @Test
    fun darkThemeUsesNight() {
        val prefs = AppPreferences(theme = ThemeChoice.Dark, atmosphere = Atmosphere.DuskBloom)
        assertEquals(Atmosphere.Night, prefs.atmosphereFor(Atmosphere.Mint, systemDark = false))
    }

    @Test
    fun autoFollowsTheSystem() {
        val prefs = AppPreferences(theme = ThemeChoice.Auto)
        assertEquals(Atmosphere.Night, prefs.atmosphereFor(Atmosphere.Mint, systemDark = true))
        assertEquals(Atmosphere.Mint, prefs.atmosphereFor(Atmosphere.Mint, systemDark = false))
    }

    @Test
    fun recentsPutTheNewestFirstWithoutDuplicatesUpToTheCap() {
        val start = listOf(StatusWidgetKind.Time, StatusWidgetKind.Wifi, StatusWidgetKind.Sound)
        assertEquals(
            listOf(StatusWidgetKind.Wifi, StatusWidgetKind.Time, StatusWidgetKind.Sound),
            start.withRecent(StatusWidgetKind.Wifi)
        )
        assertEquals(
            listOf(StatusWidgetKind.Battery, StatusWidgetKind.Time, StatusWidgetKind.Wifi),
            start.withRecent(StatusWidgetKind.Battery)
        )
    }
}
