package com.fivehouse.thewid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.ShapesTheme

/**
 * The current atmosphere's gradient, with content coloured to suit it.
 *
 * @param glass pass this to let [GlassTile]s in [content] blur the gradient.
 */
@Composable
fun AtmosphereBackground(
    modifier: Modifier = Modifier,
    glass: FrostedGlassState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val atmosphere = ShapesTheme.atmosphere
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .then(if (glass != null) Modifier.frostedGlassSource(glass) else Modifier)
                .background(atmosphere.background)
        )
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground
        ) {
            content()
        }
    }
}

@Preview(name = "All atmospheres", widthDp = 360)
@Composable
private fun AtmosphereBackgroundPreview() {
    Column {
        Atmosphere.entries.forEach { atmosphere ->
            ShapesTheme(atmosphere) {
                AtmosphereBackground(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Text(atmosphere.name, Modifier.padding(16.dp))
                }
            }
        }
    }
}
