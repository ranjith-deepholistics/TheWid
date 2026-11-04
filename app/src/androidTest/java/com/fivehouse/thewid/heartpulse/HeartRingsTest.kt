package com.fivehouse.thewid.heartpulse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartRingsTest {
    private fun drawn(progress: Float): IntArray {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        HeartRings().draw(Canvas(bitmap), 200f, 2f, Color.RED, progress)
        return IntArray(40_000).also { bitmap.getPixels(it, 0, 200, 0, 0, 200, 200) }
    }

    @Test
    fun ringsAreVisibleAtTheStartOfABeat() {
        assertTrue(drawn(0f).any { Color.alpha(it) > 100 })
    }

    @Test
    fun ringsHaveFadedByTheEndSoNothingLingersOverTheScreen() {
        assertEquals(0, drawn(1f).count { Color.alpha(it) > 0 })
    }

    @Test
    fun theMiddleStaysClearSoTheCameraIsntCovered() {
        val pixels = drawn(0f)
        assertEquals(0, Color.alpha(pixels[100 * 200 + 100]))
    }
}
