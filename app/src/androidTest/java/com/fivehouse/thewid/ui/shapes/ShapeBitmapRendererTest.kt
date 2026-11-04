package com.fivehouse.thewid.ui.shapes

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import androidx.graphics.shapes.toPath
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fivehouse.thewid.ui.theme.GlossTint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShapeBitmapRendererTest {
    private fun render(
        shape: ShapeCatalog = ShapeCatalog.Circle,
        fill: Float = 1f,
        surface: ShapeSurface = ShapeSurface.Glossy
    ): Bitmap = ShapeBitmapRenderer.render(
        shape = shape,
        sizePx = SIZE,
        density = 2f,
        tint = GlossTint.Accent,
        fill = fill,
        surface = surface
    )

    @Test
    fun everyCatalogShapeFitsTheUnitSquareAndRenders() {
        ShapeCatalog.entries.forEach { shape ->
            val bounds = RectF().also { shape.polygon.toPath(Path()).computeBounds(it, true) }
            assertTrue("$shape bounds $bounds", bounds.left >= -EPSILON && bounds.top >= -EPSILON)
            assertTrue("$shape bounds $bounds", bounds.right <= 1 + EPSILON && bounds.bottom <= 1 + EPSILON)
            assertTrue("$shape drew nothing", render(shape).hasOpaquePixel())
        }
    }

    @Test
    fun partialFillLeavesTopTranslucentAndBottomOpaque() {
        val bitmap = render(fill = 0.5f)
        val centre = SIZE / 2
        assertTrue(Color.alpha(bitmap.getPixel(centre, SIZE / 4)) < OPAQUE)
        assertEquals(OPAQUE, Color.alpha(bitmap.getPixel(centre, SIZE * 3 / 4)))
    }

    @Test
    fun outlinesLeaveTheMiddleEmpty() {
        listOf(ShapeSurface.Outline, ShapeSurface.Dashed).forEach { surface ->
            val bitmap = render(surface = surface)
            assertEquals(0, Color.alpha(bitmap.getPixel(SIZE / 2, SIZE / 2)))
            assertTrue("$surface drew nothing", bitmap.hasOpaquePixel())
        }
    }

    @Test
    fun dashedOutlineHasGapsThatSolidOutlineDoesNot() {
        val solid = render(surface = ShapeSurface.Outline).paintedPixels()
        val dashed = render(surface = ShapeSurface.Dashed).paintedPixels()
        assertTrue("dashed $dashed vs solid $solid", dashed < solid)
    }

    @Test
    fun albumArtFillsTheShapeInsteadOfTheGloss() {
        val art = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val bitmap = ShapeBitmapRenderer.render(ShapeCatalog.Circle, SIZE, 2f, GlossTint.Accent, art = art)
        assertEquals(Color.RED, bitmap.getPixel(SIZE / 2, SIZE / 2))
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
    }

    private fun Bitmap.hasOpaquePixel(): Boolean = IntArray(width * height).also {
        getPixels(it, 0, width, 0, 0, width, height)
    }.any { Color.alpha(it) == OPAQUE }

    private fun Bitmap.paintedPixels(): Int = IntArray(width * height).also {
        getPixels(it, 0, width, 0, 0, width, height)
    }.count { Color.alpha(it) > 0 }

    private companion object {
        const val SIZE = 200
        const val OPAQUE = 255
        const val EPSILON = 0.001f
    }
}
