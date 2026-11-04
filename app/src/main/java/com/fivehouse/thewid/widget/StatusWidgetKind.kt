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
    val shapeOptions: List<ShapeCatalog>
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
    Time(R.string.widget_time, TimeWidgetReceiver::class, emptyList());

    fun newWidget(): StatusWidget = when (this) {
        Battery -> BatteryWidget()
        Cellular -> CellularWidget()
        Wifi -> WifiWidget()
        Sound -> SoundWidget()
        Alarm -> AlarmWidget()
        Time -> TimeWidget()
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
