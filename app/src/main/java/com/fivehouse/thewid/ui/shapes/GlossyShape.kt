package com.fivehouse.thewid.ui.shapes

import android.graphics.Bitmap
import android.graphics.Path
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.fivehouse.thewid.ui.components.AtmosphereBackground
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme

/**
 * A library shape whose form carries data. Size it through [modifier].
 *
 * Changing [shape] morphs to the new one and changing [fill] springs to the new level.
 *
 * @param contentDescription what the shape currently shows, or null if decorative.
 * @param fill level from 0 to 1, clipped from the bottom. Only applies to [ShapeSurface.Glossy].
 * @param castShadow reserves the bottom of the box for a soft shadow. For hero shapes.
 * @param clock draws the Time face's hand and minute dot over the shape.
 * @param art fills the shape instead of the gloss, such as album art.
 * @param content drawn centred over the shape, such as the number inside Alerts.
 */
@Composable
fun GlossyShape(
    shape: ShapeCatalog,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fill: Float = 1f,
    surface: ShapeSurface = ShapeSurface.Glossy,
    tint: GlossTint = ShapesTheme.atmosphere.gloss,
    castShadow: Boolean = false,
    clock: ClockMarks? = null,
    art: Bitmap? = null,
    expression: FaceExpression? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val morphState = remember { ShapeMorphState(shape) }
    LaunchedEffect(shape) { morphState.animateTo(shape) }
    val animatedFill by animateFloatAsState(
        targetValue = fill.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "shapeFill"
    )
    val density = LocalDensity.current.density
    val painter = remember(density) { ShapePainter(density) }
    val unitPath = remember { Path() }
    val outlineColor = LocalContentColor.current

    Box(
        modifier = modifier
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                    role = Role.Image
                }
            }
            .drawBehind {
                morphState.writePath(unitPath)
                drawIntoCanvas { canvas ->
                    painter.draw(
                        canvas = canvas.nativeCanvas,
                        unitPath = unitPath,
                        width = size.width,
                        height = size.height,
                        spec = ShapePaintSpec(animatedFill, surface, tint, outlineColor, castShadow, art)
                    )
                    val box = minOf(size.width, size.height)
                    if (clock != null) painter.drawClock(canvas.nativeCanvas, box, clock)
                    if (expression != null) painter.drawExpression(canvas.nativeCanvas, box, expression)
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Rule 4: a state change is a spring-driven morph. */
@Stable
private class ShapeMorphState(initial: ShapeCatalog) {
    private var settled by mutableStateOf(initial)
    private var morph by mutableStateOf<Morph?>(null)
    private val progress = Animatable(1f)

    suspend fun animateTo(target: ShapeCatalog) {
        if (target == settled) return
        morph = Morph(settled.polygon, target.polygon)
        settled = target
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        morph = null
    }

    fun writePath(path: Path) {
        val active = morph
        if (active != null) active.toPath(progress.value, path) else settled.polygon.toPath(path)
    }
}

@Preview(name = "Rule 1: fill = level", widthDp = 360, showBackground = true)
@Composable
private fun GlossyShapeFillPreview() {
    ShapesTheme {
        AtmosphereBackground {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                    GlossyShape(ShapeCatalog.SoftBurst, null, Modifier.size(56.dp), fill = level)
                }
            }
        }
    }
}

@Preview(name = "Rule 2: outline = off", widthDp = 360, showBackground = true)
@Composable
private fun GlossyShapeSurfacePreview() {
    ShapesTheme(Atmosphere.Mint) {
        AtmosphereBackground {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShapeSurface.entries.forEach { surface ->
                    GlossyShape(ShapeCatalog.Fan, null, Modifier.size(88.dp), surface = surface)
                }
            }
        }
    }
}

@Preview(name = "Accent, shadow, content", widthDp = 360, showBackground = true)
@Composable
private fun GlossyShapeAccentPreview() {
    ShapesTheme(Atmosphere.Moss) {
        AtmosphereBackground {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlossyShape(
                    shape = ShapeCatalog.SoftBurst,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    fill = 0.68f,
                    tint = GlossTint.Accent,
                    castShadow = true
                )
                GlossyShape(ShapeCatalog.Clover4Leaf, null, Modifier.size(140.dp)) {
                    Text("2", style = Numerals.Large)
                }
            }
        }
    }
}
