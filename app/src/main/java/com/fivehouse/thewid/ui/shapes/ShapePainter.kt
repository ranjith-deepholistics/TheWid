package com.fivehouse.thewid.ui.shapes

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.toPath
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Ink

/** How a shape's surface reads. Rule 2: an outline means off or unavailable. */
enum class ShapeSurface { Glossy, Outline, Dashed }

internal data class ShapePaintSpec(
    val fill: Float,
    val surface: ShapeSurface,
    val tint: GlossTint,
    val outlineColor: Color,
    val castShadow: Boolean,
    val art: Bitmap? = null
)

/** The y a level's fill reaches: fills rise from [bottom] towards [top]. */
internal fun fillLineY(top: Float, bottom: Float, fill: Float): Float = bottom - (bottom - top) * fill.coerceIn(0f, 1f)

/** Mochi's little face, drawn over its shape. */
enum class FaceExpression { Happy, Excited, Sleepy, Dozing, Lonely, Busy }

/** Positions for a clock's Arrow hand and Diamond minute dot, clockwise from 12. */
@Immutable
data class ClockMarks(val hourDegrees: Float, val minuteDegrees: Float)

private const val CLOCK_HAND_SCALE = 0.3f
private const val CLOCK_HAND_ORBIT = 0.12f
private const val CLOCK_DOT_SCALE = 0.11f
private const val CLOCK_DOT_ORBIT = 0.42f

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

    /**
     * Draws a small solid shape around the box's centre, such as a clock hand.
     *
     * @param degrees clockwise from 12, turning the mark with it.
     * @param markScale the mark's size as a share of [boxSize].
     * @param orbit how far the mark's centre sits from the box's centre, as a share of [boxSize].
     */
    fun drawMark(
        canvas: Canvas,
        unitPath: Path,
        boxSize: Float,
        degrees: Float,
        markScale: Float,
        orbit: Float,
        color: Int
    ) {
        val mark = boxSize * markScale
        val center = boxSize / 2
        matrix.setScale(mark, mark)
        matrix.postTranslate(center - mark / 2, center - mark / 2 - boxSize * orbit)
        matrix.postRotate(degrees, center, center)
        unitPath.transform(matrix, devicePath)
        fillPaint.shader = null
        fillPaint.color = color
        canvas.drawPath(devicePath, fillPaint)
    }

    /** The Arrow hand and Diamond minute dot of the Time face, over a shape in a [boxSize] square. */
    fun drawClock(canvas: Canvas, boxSize: Float, clock: ClockMarks) {
        drawMark(
            canvas = canvas,
            unitPath = ShapeCatalog.Arrow.polygon.toPath(Path()),
            boxSize = boxSize,
            degrees = clock.hourDegrees,
            markScale = CLOCK_HAND_SCALE,
            orbit = CLOCK_HAND_ORBIT,
            color = Ink.toArgb()
        )
        drawMark(
            canvas = canvas,
            unitPath = ShapeCatalog.Diamond.polygon.toPath(Path()),
            boxSize = boxSize,
            degrees = clock.minuteDegrees,
            markScale = CLOCK_DOT_SCALE,
            orbit = CLOCK_DOT_ORBIT,
            color = Accent.toArgb()
        )
    }

    /** Draws eyes and a mouth in Ink, proportioned to a [boxSize] square. */
    fun drawExpression(canvas: Canvas, boxSize: Float, expression: FaceExpression) {
        strokePaint.pathEffect = null
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeWidth = boxSize * 0.03f
        strokePaint.color = Ink.toArgb()
        fillPaint.shader = null
        fillPaint.color = Ink.toArgb()
        val left = boxSize * 0.38f
        val right = boxSize * 0.62f
        val eyeY = boxSize * 0.44f
        val eye = boxSize * 0.045f
        val dotEyes = {
            canvas.drawCircle(left, eyeY, boxSize * 0.028f, fillPaint)
            canvas.drawCircle(right, eyeY, boxSize * 0.028f, fillPaint)
        }
        val flatEyes = {
            canvas.drawLine(left - eye, eyeY, left + eye, eyeY, strokePaint)
            canvas.drawLine(right - eye, eyeY, right + eye, eyeY, strokePaint)
        }
        val mouthY = boxSize * 0.58f
        val mouth = RectF(boxSize * 0.42f, mouthY - boxSize * 0.06f, boxSize * 0.58f, mouthY + boxSize * 0.06f)
        when (expression) {
            FaceExpression.Happy -> {
                dotEyes()
                canvas.drawArc(mouth, 20f, 140f, false, strokePaint)
            }

            FaceExpression.Excited -> {
                listOf(left, right).forEach { x ->
                    canvas.drawLine(x - eye, eyeY + eye / 2, x, eyeY - eye / 2, strokePaint)
                    canvas.drawLine(x, eyeY - eye / 2, x + eye, eyeY + eye / 2, strokePaint)
                }
                canvas.drawArc(mouth, 0f, 180f, true, fillPaint)
            }

            FaceExpression.Sleepy -> {
                flatEyes()
                canvas.drawCircle(boxSize / 2, mouthY, boxSize * 0.02f, fillPaint)
            }

            FaceExpression.Dozing -> {
                flatEyes()
                canvas.drawArc(mouth, 40f, 100f, false, strokePaint)
            }

            FaceExpression.Lonely -> {
                dotEyes()
                mouth.offset(0f, boxSize * 0.05f)
                canvas.drawArc(mouth, 200f, 140f, false, strokePaint)
            }

            FaceExpression.Busy -> {
                dotEyes()
                canvas.drawLine(boxSize * 0.44f, mouthY, boxSize * 0.56f, mouthY, strokePaint)
            }
        }
        strokePaint.strokeCap = Paint.Cap.BUTT
    }

    /** Fills the shape with [art], centre-cropped, in place of the glossy gradient. */
    private fun drawArt(canvas: Canvas, art: Bitmap) {
        val scale = maxOf(bounds.width() / art.width, bounds.height() / art.height)
        val shader = BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            bounds.centerX() - art.width * scale / 2,
            bounds.centerY() - art.height * scale / 2
        )
        shader.setLocalMatrix(matrix)
        fillPaint.alpha = OPAQUE
        fillPaint.shader = shader
        canvas.drawPath(devicePath, fillPaint)
        fillPaint.shader = null
        strokePaint.pathEffect = null
        strokePaint.strokeWidth = RIM_WIDTH_DP * density
        strokePaint.color = RIM.toArgb()
        canvas.drawPath(devicePath, strokePaint)
    }

    private fun drawGlossy(canvas: Canvas, spec: ShapePaintSpec) {
        if (spec.art != null) {
            drawArt(canvas, spec.art)
            return
        }
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
