package com.fivehouse.thewid.ui.shapes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.graphics.shapes.toPath
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
        if (clock != null) painter.drawClock(canvas, size, clock)
        return bitmap
    }
}
