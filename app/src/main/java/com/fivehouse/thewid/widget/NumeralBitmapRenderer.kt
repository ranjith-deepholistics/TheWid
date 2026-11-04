package com.fivehouse.thewid.widget

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import androidx.core.graphics.createBitmap
import com.fivehouse.thewid.R
import kotlin.math.ceil

/** Renders Doto text to a [Bitmap], because Glance can only set system fonts on its text. */
object NumeralBitmapRenderer {
    private const val DOTO_WEIGHT = 900

    @Volatile
    private var typeface: Typeface? = null

    private fun doto(resources: Resources): Typeface = typeface ?: run {
        val font = Font.Builder(resources, R.font.doto)
            .setFontVariationSettings("'wght' $DOTO_WEIGHT")
            .setWeight(DOTO_WEIGHT)
            .build()
        Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).build().also { typeface = it }
    }

    fun render(resources: Resources, text: String, sizePx: Float, color: Int): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = doto(resources)
            textSize = sizePx
            this.color = color
        }
        val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        val width = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val height = bounds.height().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        Canvas(bitmap).drawText(text, 0f, -bounds.top.toFloat(), paint)
        return bitmap
    }
}
