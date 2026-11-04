package com.fivehouse.thewid.heartpulse

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.graphics.shapes.toPath
import com.fivehouse.thewid.data.personal.HeartPulseSettings
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.widget.personalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BEAT_MS = 1_400L
private const val RING_COUNT = 3
private const val RING_STEP = 0.32f
private const val MIN_WINDOW_DP = 140
private const val CUTOUT_SCALE = 4
private const val FALLBACK_CENTER_Y_DP = 16

/** Android lets touches through an overlay only at or below this opacity. Mockup 8 promises they pass. */
private const val MAX_TOUCH_THROUGH_ALPHA = 0.8f

enum class PulseTrigger { Notification, Charging, Breathe, Hour, Test }

/** Whether something should beat. Nothing shows without permission, or to a screen nobody can see. */
fun shouldPulse(
    settings: HeartPulseSettings,
    trigger: PulseTrigger,
    canDrawOverlays: Boolean,
    isInteractive: Boolean
): Boolean {
    if (!canDrawOverlays || !isInteractive) return false
    if (trigger == PulseTrigger.Test) return true
    if (!settings.enabled) return false
    return when (trigger) {
        PulseTrigger.Notification -> settings.onNotification
        PulseTrigger.Charging -> settings.onCharging
        PulseTrigger.Breathe -> settings.duringBreathe
        PulseTrigger.Hour -> settings.everyHour
        PulseTrigger.Test -> true
    }
}

/** A square window's top-left corner, centring it on a point. */
fun pulseWindowOrigin(centerX: Int, centerY: Int, sizePx: Int): Pair<Int, Int> =
    (centerX - sizePx / 2) to (centerY - sizePx / 2)

/**
 * Draws heart rings with the real library Heart. The overlay and the setup screen's preview both
 * use this, so what you see while choosing is what beats around the camera.
 */
class HeartRings {
    private val unit = ShapeCatalog.Heart.polygon.toPath(Path())
    private val scaled = Path()
    private val matrix = Matrix()
    private val bounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    /** @param progress how far through a beat, from 0 to 1. The rings swell and fade across it. */
    fun draw(canvas: Canvas, size: Float, density: Float, color: Int, progress: Float) {
        unit.computeBounds(bounds, true)
        val base = size / (1 + RING_STEP * RING_COUNT)
        paint.color = color
        paint.strokeWidth = 2f * density
        repeat(RING_COUNT) { ring ->
            val scale = base * (1 + ring * RING_STEP + progress * RING_STEP * 0.5f) / bounds.width()
            matrix.setScale(scale, scale)
            matrix.postTranslate(
                size / 2 - bounds.centerX() * scale,
                size / 2 - bounds.centerY() * scale
            )
            unit.transform(matrix, scaled)
            val fade = (1f - progress) * (1f - ring.toFloat() / RING_COUNT)
            paint.alpha = (255 * fade).toInt().coerceIn(0, 255)
            canvas.drawPath(scaled, paint)
        }
    }
}

// Only ever created in code, never inflated from a layout.
@SuppressLint("ViewConstructor")
private class HeartPulseView(context: Context, private val color: Int) : View(context) {
    private val rings = HeartRings()
    var progress = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        rings.draw(canvas, width.toFloat(), resources.displayMetrics.density, color, progress)
    }
}

/** Shows one beat around the camera in a small untouchable window, then removes it. */
object HeartPulse {
    private var showing = false

    fun trigger(context: Context, trigger: PulseTrigger) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            val settings = app.personalRepository().heartPulse.first()
            val allowed = shouldPulse(
                settings,
                trigger,
                Settings.canDrawOverlays(app),
                app.getSystemService(PowerManager::class.java).isInteractive
            )
            if (allowed) withContext(Dispatchers.Main) { beat(app, settings.color.argb) }
        }
    }

    private fun beat(context: Context, color: Int) {
        if (showing) return
        val manager = context.getSystemService(WindowManager::class.java)
        val metrics = manager.currentWindowMetrics
        val density = context.resources.displayMetrics.density
        val cutout = metrics.windowInsets.displayCutout?.boundingRects?.firstOrNull()
        val centerX = cutout?.centerX() ?: (metrics.bounds.width() / 2)
        val centerY = cutout?.centerY() ?: (FALLBACK_CENTER_Y_DP * density).toInt()
        val size = maxOf(
            (MIN_WINDOW_DP * density).toInt(),
            (cutout?.let { maxOf(it.width(), it.height()) } ?: 0) * CUTOUT_SCALE
        )
        val (x, y) = pulseWindowOrigin(centerX, centerY, size)
        val params = WindowManager.LayoutParams(
            size,
            size,
            x,
            y,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = MAX_TOUCH_THROUGH_ALPHA
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "HeartPulse"
        }
        val view = HeartPulseView(context, color)
        try {
            manager.addView(view, params)
        } catch (_: WindowManager.BadTokenException) {
            return // Permission was revoked between the check and this call.
        } catch (_: SecurityException) {
            return
        }
        showing = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = BEAT_MS
            addUpdateListener { view.progress = it.animatedValue as Float }
            doOnEnd {
                manager.removeView(view)
                showing = false
            }
            start()
        }
    }

    private fun hourlyIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, HourlyPulseReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** A non-waking, inexact alarm: it beats near the hour, and only while the phone is awake. */
    fun syncHourly(context: Context, settings: HeartPulseSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (settings.enabled && settings.everyHour) {
            val hour = AlarmManager.INTERVAL_HOUR
            val next = (System.currentTimeMillis() / hour + 1) * hour
            alarms.setInexactRepeating(AlarmManager.RTC, next, hour, hourlyIntent(context))
        } else {
            alarms.cancel(hourlyIntent(context))
        }
    }
}

class HourlyPulseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        HeartPulse.trigger(context, PulseTrigger.Hour)
    }
}
