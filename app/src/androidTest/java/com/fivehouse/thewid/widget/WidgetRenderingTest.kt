package com.fivehouse.thewid.widget

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.ui.shapes.ClockMarks
import com.fivehouse.thewid.ui.shapes.ShapeBitmapRenderer
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.GlossTint
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetRenderingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun Bitmap.pixels() = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

    @Test
    fun dotoNumeralsDrawSomething() {
        val bitmap = NumeralBitmapRenderer.render(context.resources, "68%", 72f, Color.BLACK)
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
        assertTrue(bitmap.pixels().any { Color.alpha(it) > 0 })
    }

    @Test
    fun clockMarksChangeTheDrawing() {
        fun render(clock: ClockMarks?) = ShapeBitmapRenderer.render(
            shape = ShapeCatalog.Sunny,
            sizePx = 200,
            density = 2f,
            tint = GlossTint.Sun,
            clock = clock
        )
        assertTrue(!render(null).sameAs(render(ClockMarks(90f, 0f))))
        assertTrue(!render(ClockMarks(90f, 0f)).sameAs(render(ClockMarks(180f, 0f))))
    }

    @Test
    fun timeFaceUsesAClock() {
        val face = TimeStatus(ZonedDateTime.now(), isDay = true).toFace()
        assertEquals(FaceValue.LiveClock, face.value)
    }

    @Test
    fun everyKindProducesAFaceFromRealSources() {
        val faces = context.statusFaces()
        StatusWidgetKind.entries.forEach { kind ->
            val face = runBlocking { withTimeout(5_000) { faces.faces(kind, null).first() } }
            assertTrue("$kind fill ${face.fill}", face.fill in 0f..1f)
        }
    }

    @Test
    fun everyKindHasARegisteredProvider() {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val providers = manager.installedProviders.map { it.provider.className }
        StatusWidgetKind.entries.forEach { kind ->
            assertTrue("$kind not registered", kind.receiver.java.name in providers)
        }
    }
}
