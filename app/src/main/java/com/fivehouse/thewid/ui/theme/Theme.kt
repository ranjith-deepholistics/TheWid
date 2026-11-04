package com.fivehouse.thewid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LocalAtmosphere = staticCompositionLocalOf { Atmosphere.CoolMist }

private val ShapesShapes = Shapes(
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private fun colorSchemeFor(atmosphere: Atmosphere): ColorScheme {
    val surface = atmosphere.stops[atmosphere.stops.size / 2]
    return if (atmosphere.isDark) {
        darkColorScheme(
            primary = DarkAtmosphereText,
            onPrimary = Ink,
            tertiary = Accent,
            onTertiary = Color.White,
            background = surface,
            onBackground = DarkAtmosphereText,
            surface = surface,
            onSurface = DarkAtmosphereText,
            onSurfaceVariant = DarkAtmosphereText.copy(alpha = 0.72f),
            outline = DarkAtmosphereText.copy(alpha = 0.72f)
        )
    } else {
        lightColorScheme(
            primary = Ink,
            onPrimary = Color.White,
            tertiary = Accent,
            onTertiary = Color.White,
            background = surface,
            onBackground = Ink,
            surface = surface,
            onSurface = Ink,
            onSurfaceVariant = SecondaryText,
            outline = SecondaryText
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapesTheme(atmosphere: Atmosphere = Atmosphere.CoolMist, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAtmosphere provides atmosphere) {
        MaterialExpressiveTheme(
            colorScheme = colorSchemeFor(atmosphere),
            motionScheme = MotionScheme.expressive(),
            shapes = ShapesShapes,
            typography = ShapesTypography,
            content = content
        )
    }
}

object ShapesTheme {
    val atmosphere: Atmosphere
        @Composable
        @ReadOnlyComposable
        get() = LocalAtmosphere.current
}
