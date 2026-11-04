package com.fivehouse.thewid.ui.shapes

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Ink

/** How a shape's surface reads. Rule 2: an outline means off or unavailable. */
enum class ShapeSurface { Glossy, Outline, Dashed }

internal data class ShapePaintSpec(
    val fill: Float,
    val surface: ShapeSurface,
    val tint: GlossTint,
    val outlineColor: Color,
    val castShadow: Boolean
)

/** The y a level's fill reaches: fills rise from [bottom] towards [top]. */
internal fun fillLineY(top: Float, bottom: Float, fill: Float): Float = bottom - (bottom - top) * fill.coerceIn(0f, 1f)

// Share of the box the shape keeps when a shadow needs room beneath it.
private const val SHADOW_SHAPE_SCALE = 0.86f
private const val GLOSS_CENTER_X = 0.35f
private const val GLOSS_CENTER_Y = 0.28f
private const val GLOSS_RADIUS = 0.9f
private const val GLOSS_LIGHT_STOP = 0.55f

/**
 * Draws a shape onto an Android [Canvas]. Both the in-app composable and the Glance bitmap
 * go through here, so a widget always matches its preview. Instances hold paint objects
 * and aren't thread-safe.
 */
internal class ShapePainter(private val density: Float) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val dash = DashPathEffect(floatArrayOf(5f * density, 4f * density), 0f)
    private val matrix = Matrix()
    private val devicePath = Path()
    private val bounds = RectF()

    /** [unitPath] must fit the unit square, as `MaterialShapes` polygons do. */
    fun draw(canvas: Canvas, unitPath: Path, width: Float, height: Float, spec: ShapePaintSpec) {
        val available = minOf(width, height)
        val box = if (spec.castShadow) available * SHADOW_SHAPE_SCALE else available
        val inset = OUTLINE_WIDTH_DP * density
        matrix.setScale(box - 2 * inset, box - 2 * inset)
        matrix.postTranslate((width - box) / 2 + inset, inset)
        unitPath.transform(matrix, devicePath)
        devicePath.computeBounds(bounds, true)

        if (spec.castShadow && spec.surface == ShapeSurface.Glossy) {
            drawShadow(canvas, width, top = box, bottom = available)
        }
        when (spec.surface) {
            ShapeSurface.Glossy -> drawGlossy(canvas, spec)
            ShapeSurface.Outline -> drawOutline(canvas, spec.outlineColor, dashed = false)
            ShapeSurface.Dashed -> drawOutline(canvas, spec.outlineColor, dashed = true)
        }
    }

    private fun drawGlossy(canvas: Canvas, spec: ShapePaintSpec) {
        if (spec.fill < 1f) {
            fillPaint.shader = null
            fillPaint.color = EMPTY_WELL.toArgb()
            canvas.drawPath(devicePath, fillPaint)
        }
        if (spec.fill > 0f) {
            fillPaint.alpha = OPAQUE // Paint alpha multiplies a shader.
            fillPaint.shader = RadialGradient(
                bounds.left + bounds.width() * GLOSS_CENTER_X,
                bounds.top + bounds.height() * GLOSS_CENTER_Y,
                maxOf(bounds.width(), bounds.height()) * GLOSS_RADIUS,
                intArrayOf(Color.White.toArgb(), spec.tint.light.toArgb(), spec.tint.deep.toArgb()),
                floatArrayOf(0f, GLOSS_LIGHT_STOP, 1f),
                Shader.TileMode.CLAMP
            )
            val save = canvas.save()
            canvas.clipRect(
                bounds.left,
                fillLineY(bounds.top, bounds.bottom, spec.fill),
                bounds.right,
                bounds.bottom
            )
            canvas.drawPath(devicePath, fillPaint)
            canvas.restoreToCount(save)
            fillPaint.shader = null
        }
        strokePaint.pathEffect = null
        strokePaint.strokeWidth = RIM_WIDTH_DP * density
        strokePaint.color = RIM.toArgb()
        canvas.drawPath(devicePath, strokePaint)
    }

    private fun drawOutline(canvas: Canvas, color: Color, dashed: Boolean) {
        strokePaint.pathEffect = if (dashed) dash else null
        strokePaint.strokeWidth = OUTLINE_WIDTH_DP * density
        strokePaint.color = color.toArgb()
        canvas.drawPath(devicePath, strokePaint)
    }

    private fun drawShadow(canvas: Canvas, width: Float, top: Float, bottom: Float) {
        val centerX = width / 2
        val centerY = (top + bottom) / 2
        val radiusX = bounds.width() * 0.42f
        val radiusY = (bottom - top) / 2
        val shader = RadialGradient(
            0f,
            0f,
            1f,
            SHADOW.toArgb(),
            Color.Transparent.toArgb(),
            Shader.TileMode.CLAMP
        )
        matrix.setScale(radiusX, radiusY)
        matrix.postTranslate(centerX, centerY)
        shader.setLocalMatrix(matrix)
        fillPaint.alpha = OPAQUE
        fillPaint.shader = shader
        canvas.drawOval(centerX - radiusX, top, centerX + radiusX, bottom, fillPaint)
        fillPaint.shader = null
    }

    private companion object {
        const val OPAQUE = 255
        const val RIM_WIDTH_DP = 1f
        const val OUTLINE_WIDTH_DP = 1.5f
        val RIM = Color.White.copy(alpha = 0.75f)
        val EMPTY_WELL = Color.White.copy(alpha = 0.3f)
        val SHADOW = Ink.copy(alpha = 0.22f)
    }
}
