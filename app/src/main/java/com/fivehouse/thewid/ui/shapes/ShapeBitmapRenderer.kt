package com.fivehouse.thewid.ui.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.toPath
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Ink

/** Renders a shape to a [Bitmap] for Glance, which can't draw a `RoundedPolygon` itself. */
object ShapeBitmapRenderer {
    /**
     * @param sizePx width and height of the square bitmap.
     * @param density display density, so strokes match the in-app shape. Pass `Resources.displayMetrics.density`.
     * @param fill level from 0 to 1. Values outside the range are clamped.
     */
    fun render(
        shape: ShapeCatalog,
        sizePx: Int,
        density: Float,
        tint: GlossTint,
        fill: Float = 1f,
        surface: ShapeSurface = ShapeSurface.Glossy,
        outlineColor: Color = Ink,
        castShadow: Boolean = false,
        clock: ClockMarks? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val painter = ShapePainter(density)
        val size = sizePx.toFloat()
        painter.draw(
            canvas = canvas,
            unitPath = shape.polygon.toPath(Path()),
            width = size,
            height = size,
            spec = ShapePaintSpec(fill.coerceIn(0f, 1f), surface, tint, outlineColor, castShadow)
        )
        if (clock != null) {
            painter.drawMark(
                canvas = canvas,
                unitPath = ShapeCatalog.Arrow.polygon.toPath(Path()),
                boxSize = size,
                degrees = clock.hourDegrees,
                markScale = CLOCK_HAND_SCALE,
                orbit = CLOCK_HAND_ORBIT,
                color = Ink.toArgb()
            )
            painter.drawMark(
                canvas = canvas,
                unitPath = ShapeCatalog.Diamond.polygon.toPath(Path()),
                boxSize = size,
                degrees = clock.minuteDegrees,
                markScale = CLOCK_DOT_SCALE,
                orbit = CLOCK_DOT_ORBIT,
                color = Accent.toArgb()
            )
        }
        return bitmap
    }
}

/** Positions for a clock's Arrow hand and Diamond minute dot, clockwise from 12. */
@Immutable
data class ClockMarks(val hourDegrees: Float, val minuteDegrees: Float)

private const val CLOCK_HAND_SCALE = 0.3f
private const val CLOCK_HAND_ORBIT = 0.12f
private const val CLOCK_DOT_SCALE = 0.11f
private const val CLOCK_DOT_ORBIT = 0.42f
