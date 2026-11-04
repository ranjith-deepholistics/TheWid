package com.fivehouse.thewid.widget

import androidx.annotation.StringRes
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import kotlin.reflect.KClass

/**
 * @property shapeOptions shapes a user can choose, with the spec's shape first. Empty where the
 * shape itself reports the state, as with Sound and Time.
 */
enum class StatusWidgetKind(
    @param:StringRes val label: Int,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
    val shapeOptions: List<ShapeCatalog>,
    val needsNotificationAccess: Boolean = false
) {
    Battery(
        R.string.widget_battery,
        BatteryWidgetReceiver::class,
        listOf(
            ShapeCatalog.SoftBurst,
            ShapeCatalog.Cookie9Sided,
            ShapeCatalog.Circle,
            ShapeCatalog.Pentagon,
            ShapeCatalog.Cookie12Sided
        )
    ),
    Cellular(
        R.string.widget_cellular,
        CellularWidgetReceiver::class,
        listOf(
            ShapeCatalog.Pentagon,
            ShapeCatalog.Gem,
            ShapeCatalog.Circle,
            ShapeCatalog.Cookie6Sided,
            ShapeCatalog.Diamond
        )
    ),
    Wifi(
        R.string.widget_wifi,
        WifiWidgetReceiver::class,
        listOf(
            ShapeCatalog.Fan,
            ShapeCatalog.Arch,
            ShapeCatalog.Circle,
            ShapeCatalog.ClamShell,
            ShapeCatalog.Bun
        )
    ),
    Sound(R.string.widget_sound, SoundWidgetReceiver::class, emptyList()),
    Alarm(
        R.string.widget_alarm,
        AlarmWidgetReceiver::class,
        listOf(
            ShapeCatalog.SemiCircle,
            ShapeCatalog.Arch,
            ShapeCatalog.Circle,
            ShapeCatalog.Bun,
            ShapeCatalog.Oval
        )
    ),
    Time(R.string.widget_time, TimeWidgetReceiver::class, emptyList()),
    Alerts(R.string.widget_alerts, AlertsWidgetReceiver::class, emptyList(), needsNotificationAccess = true),
    NowPlaying(
        R.string.widget_now_playing,
        NowPlayingWidgetReceiver::class,
        emptyList(),
        needsNotificationAccess = true
    ),
    Week(R.string.widget_week, WeekWidgetReceiver::class, emptyList()),
    InnerCircle(R.string.widget_inner_circle, InnerCircleWidgetReceiver::class, emptyList()),
    Decide(R.string.widget_decide, DecideWidgetReceiver::class, emptyList()),
    Mochi(R.string.widget_mochi, MochiWidgetReceiver::class, emptyList()),
    Breathe(R.string.widget_breathe, BreatheWidgetReceiver::class, emptyList()),
    Focus(R.string.widget_focus, FocusWidgetReceiver::class, emptyList());

    /** These open a setup screen before Customize, because they show nothing until they're filled in. */
    val needsSetup: Boolean get() = this == Week || this == InnerCircle || this == Decide || this == Mochi

    /** Tapping the widget opens this kind's own screen rather than the Library. */
    val opensOwnScreen: Boolean
        get() = when (this) {
            Battery, Cellular, Wifi, Sound, Alarm, Alerts -> false
            else -> true
        }

    fun newWidget(): StatusWidget = when (this) {
        Battery -> BatteryWidget()
        Cellular -> CellularWidget()
        Wifi -> WifiWidget()
        Sound -> SoundWidget()
        Alarm -> AlarmWidget()
        Time -> TimeWidget()
        Alerts -> AlertsWidget()
        NowPlaying -> NowPlayingWidget()
        Week -> WeekWidget()
        InnerCircle -> InnerCircleWidget()
        Decide -> DecideWidget()
        Mochi -> MochiWidget()
        Breathe -> BreatheWidget()
        Focus -> FocusWidget()
    }

    companion object {
        fun forReceiverClass(className: String): StatusWidgetKind? =
            entries.firstOrNull { it.receiver.java.name == className }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Battery.newWidget()
}

class CellularWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Cellular.newWidget()
}

class WifiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Wifi.newWidget()
}

class SoundWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Sound.newWidget()
}

class AlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Alarm.newWidget()
}

class TimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Time.newWidget()
}

class AlertsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Alerts.newWidget()
}

class NowPlayingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.NowPlaying.newWidget()
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Week.newWidget()
}

class InnerCircleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.InnerCircle.newWidget()
}

class DecideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Decide.newWidget()
}

class MochiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Mochi.newWidget()
}

class BreatheWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Breathe.newWidget()
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StatusWidgetKind.Focus.newWidget()
}
