package com.fivehouse.thewid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme

object GlassTileDefaults {
    val ContentPadding = PaddingValues(16.dp)

    /** The spec's 35–45% white on light atmospheres. Dark values follow the Moss mockups, where that would wash out. */
    fun fill(atmosphere: Atmosphere): Color = Color.White.copy(alpha = if (atmosphere.isDark) 0.08f else 0.4f)

    fun border(atmosphere: Atmosphere): Color = Color.White.copy(alpha = if (atmosphere.isDark) 0.2f else 0.75f)
}

/**
 * The frosted surface everything sits on. Without [glass] it draws the same wash and border
 * with no blur, which is also how widgets fake frost on the home screen.
 */
@Composable
fun GlassTile(
    modifier: Modifier = Modifier,
    glass: FrostedGlassState? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = GlassTileDefaults.ContentPadding,
    content: @Composable BoxScope.() -> Unit
) {
    val atmosphere = ShapesTheme.atmosphere
    val fill = GlassTileDefaults.fill(atmosphere)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (glass != null) Modifier.frostedGlass(glass, fill) else Modifier.background(fill))
            .border(1.dp, GlassTileDefaults.border(atmosphere), shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
private fun GlassTileSample(atmosphere: Atmosphere, blurred: Boolean) {
    ShapesTheme(atmosphere) {
        val glass = rememberFrostedGlassState()
        AtmosphereBackground(glass = glass) {
            Box(Modifier.padding(24.dp)) {
                GlossyShape(
                    shape = ShapeCatalog.SoftBurst,
                    contentDescription = null,
                    modifier = Modifier
                        .size(110.dp)
                        .frostedGlassSource(glass)
                )
                GlassTile(
                    modifier = Modifier
                        .padding(start = 48.dp, top = 40.dp)
                        .fillMaxWidth()
                        .height(120.dp),
                    glass = if (blurred) glass else null
                ) {
                    Text("68%", Modifier.align(Alignment.BottomStart), style = Numerals.Medium)
                }
            }
        }
    }
}

@Preview(name = "Blurred and unblurred", widthDp = 360, showBackground = true)
@Composable
private fun GlassTilePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        GlassTileSample(Atmosphere.CoolMist, blurred = true)
        GlassTileSample(Atmosphere.CoolMist, blurred = false)
    }
}

@Preview(name = "Dark atmosphere", widthDp = 360, showBackground = true)
@Composable
private fun GlassTileDarkPreview() {
    GlassTileSample(Atmosphere.Moss, blurred = true)
}
