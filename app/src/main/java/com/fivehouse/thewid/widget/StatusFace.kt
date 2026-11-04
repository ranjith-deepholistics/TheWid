package com.fivehouse.thewid.widget

import android.graphics.Bitmap
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.MinuteMarker
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.SunTimes
import com.fivehouse.thewid.data.personal.TimeSettings
import com.fivehouse.thewid.data.status.AlarmStatus
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.CellularNetwork
import com.fivehouse.thewid.data.status.CellularStatus
import com.fivehouse.thewid.data.status.MAX_SIGNAL_BARS
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.data.status.WifiStatus
import com.fivehouse.thewid.ui.shapes.ClockMarker
import com.fivehouse.thewid.ui.shapes.ClockMarks
import com.fivehouse.thewid.ui.shapes.FaceExpression
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val NO_VALUE = "--"

/** Tint for faces with no colour of their own. A widget's chosen atmosphere replaces it when drawn. */
val WidgetTint = Atmosphere.CoolMist.gloss

sealed interface FaceValue {
    data class Literal(val text: String) : FaceValue

    data class Resource(@param:StringRes val id: Int) : FaceValue

    data class Plural(@param:PluralsRes val id: Int, val count: Int) : FaceValue

    data class Format(@param:StringRes val id: Int, val args: List<Int>) : FaceValue

    /** A timer the launcher keeps counting between widget updates. */
    data class Countdown(val leftMs: Long, val isRunning: Boolean) : FaceValue

    data object LiveClockSeconds : FaceValue

    /** A running clock that the launcher keeps current between widget updates. */
    data object LiveClock : FaceValue
}

/** What a status widget draws, independent of its size. */
data class StatusFace(
    val shape: ShapeCatalog,
    val value: FaceValue,
    val fill: Float = 1f,
    val surface: ShapeSurface = ShapeSurface.Glossy,
    val tint: GlossTint = WidgetTint,
    val clock: ClockMarks? = null,
    val isLive: Boolean = false,
    val valueInside: Boolean = false,
    val needsAccess: Boolean = false,
    val caption: String? = null,
    val art: Bitmap? = null,
    val expression: FaceExpression? = null,
    val strip: List<ShapeCatalog>? = null,
    val people: List<Person>? = null,
    val rollable: Boolean = false,
    val footnote: FaceValue? = null,
    val track: Float? = null
)

fun BatteryStatus.toFace(shape: ShapeCatalog = ShapeCatalog.SoftBurst, glow: Boolean = true): StatusFace {
    val level = percent ?: return StatusFace(shape, FaceValue.Literal(NO_VALUE), surface = ShapeSurface.Dashed)
    return StatusFace(
        shape = shape,
        value = FaceValue.Literal("$level%"),
        fill = level / 100f,
        tint = if (isCharging && glow) GlossTint.Accent else WidgetTint,
        isLive = isCharging
    )
}

fun CellularStatus.toFace(shape: ShapeCatalog = ShapeCatalog.Pentagon): StatusFace = when (this) {
    CellularStatus.Unavailable -> StatusFace(shape, FaceValue.Literal(NO_VALUE), surface = ShapeSurface.Dashed)

    is CellularStatus.Signal -> StatusFace(
        shape = shape,
        value = FaceValue.Literal(network?.label ?: "$bars/$MAX_SIGNAL_BARS"),
        fill = bars.toFloat() / MAX_SIGNAL_BARS
    )
}

private val CellularNetwork.label: String
    get() = when (this) {
        CellularNetwork.TwoG -> "2G"
        CellularNetwork.ThreeG -> "3G"
        CellularNetwork.FourG -> "4G"
        CellularNetwork.FiveG -> "5G"
    }

fun WifiStatus.toFace(shape: ShapeCatalog = ShapeCatalog.Fan): StatusFace = when (this) {
    WifiStatus.Off -> StatusFace(shape, FaceValue.Resource(R.string.widget_value_off), surface = ShapeSurface.Dashed)

    is WifiStatus.Connected -> StatusFace(
        shape = shape,
        value = FaceValue.Resource(R.string.widget_value_on),
        fill = bars?.let { it.toFloat() / MAX_SIGNAL_BARS } ?: 1f
    )
}

fun SoundStatus.toFace(): StatusFace = when (this) {
    SoundStatus.Ring -> StatusFace(ShapeCatalog.Circle, FaceValue.Resource(R.string.widget_value_ring))
    SoundStatus.Vibrate -> StatusFace(ShapeCatalog.Cookie12Sided, FaceValue.Resource(R.string.widget_value_vibrate))
    SoundStatus.Silent -> StatusFace(ShapeCatalog.Ghostish, FaceValue.Resource(R.string.widget_value_mute))
}

fun AlarmStatus.toFace(zone: ZoneId, is24Hour: Boolean, shape: ShapeCatalog = ShapeCatalog.SemiCircle): StatusFace =
    when (this) {
        AlarmStatus.None -> StatusFace(shape, FaceValue.Literal("--:--"), surface = ShapeSurface.Dashed)

        is AlarmStatus.Scheduled -> StatusFace(shape, FaceValue.Literal(at.format(zone, is24Hour)))
    }

private fun Instant.format(zone: ZoneId, is24Hour: Boolean): String =
    DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm").withZone(zone).format(this)

/** @param sun today's times, when "Switch at sunset" is on and a position is known. */
fun TimeStatus.toFace(settings: TimeSettings = TimeSettings(), sun: SunTimes? = null): StatusFace {
    val minute = dateTime.minute
    val dayNow = if (sun != null) !dateTime.isBefore(sun.sunrise) && dateTime.isBefore(sun.sunset) else isDay
    return StatusFace(
        shape = if (dayNow) ShapeCatalog.Sunny else ShapeCatalog.Cookie12Sided,
        value = if (settings.seconds) FaceValue.LiveClockSeconds else FaceValue.LiveClock,
        tint = if (dayNow) GlossTint.Sun else WidgetTint,
        clock = ClockMarks(
            hourDegrees = (dateTime.hour % 12 + minute / 60f) * 30f,
            minuteDegrees = minute * 6f,
            marker = when (settings.marker) {
                MinuteMarker.Diamond -> ClockMarker.Diamond
                MinuteMarker.Circle -> ClockMarker.Circle
                MinuteMarker.None -> ClockMarker.None
            }
        ),
        track = if (settings.showDayTrack) dayProgress(sun) else null,
        footnote = sun?.takeIf { settings.showDayTrack && dayNow }?.let { sunsetFootnote(it) }
    )
}

/** How far through daylight it is, from 0 to 1. Without sun times, daylight is taken as 06:00 to 18:00. */
private fun TimeStatus.dayProgress(sun: SunTimes?): Float {
    val minutes = dateTime.hour * 60 + dateTime.minute
    val start = sun?.sunrise?.let { it.hour * 60 + it.minute } ?: (6 * 60)
    val end = sun?.sunset?.let { it.hour * 60 + it.minute } ?: (18 * 60)
    return ((minutes - start).toFloat() / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
}

private fun TimeStatus.sunsetFootnote(sun: SunTimes): FaceValue {
    val left = ChronoUnit.MINUTES.between(dateTime, sun.sunset).toInt().coerceAtLeast(0)
    return FaceValue.Format(R.string.time_sunset_in, listOf(left / 60, left % 60))
}

fun AlertsStatus.toFace(): StatusFace = when (this) {
    AlertsStatus.NoAccess -> StatusFace(
        ShapeCatalog.Clover4Leaf,
        FaceValue.Literal(NO_VALUE),
        surface = ShapeSurface.Dashed,
        needsAccess = true
    )

    is AlertsStatus.Count -> StatusFace(ShapeCatalog.Clover4Leaf, FaceValue.Literal("$count"), valueInside = true)
}

fun NowPlayingStatus.toFace(): StatusFace = when (this) {
    NowPlayingStatus.NoAccess -> StatusFace(
        ShapeCatalog.Cookie12Sided,
        FaceValue.Literal(NO_VALUE),
        surface = ShapeSurface.Dashed,
        needsAccess = true
    )

    NowPlayingStatus.Nothing -> StatusFace(
        ShapeCatalog.Cookie12Sided,
        FaceValue.Literal(NO_VALUE),
        surface = ShapeSurface.Dashed
    )

    is NowPlayingStatus.Track -> StatusFace(
        shape = if (isPlaying) ShapeCatalog.Cookie12Sided else ShapeCatalog.Square,
        value = leftMs?.let { FaceValue.Countdown(it, isPlaying) }
            ?: FaceValue.Resource(if (isPlaying) R.string.widget_value_playing else R.string.widget_value_paused),
        isLive = isPlaying,
        caption = title,
        art = art
    )
}
