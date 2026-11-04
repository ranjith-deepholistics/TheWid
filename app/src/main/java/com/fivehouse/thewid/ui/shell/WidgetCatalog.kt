package com.fivehouse.thewid.ui.shell

import androidx.annotation.StringRes
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.widget.StatusWidgetKind

/**
 * Every widget in the spec.
 *
 * @property kind the working widget, or null for one a later phase builds.
 * @property needsPermission shows a lock on its tile.
 */
enum class CatalogWidget(
    @param:StringRes val label: Int,
    val shape: ShapeCatalog,
    val kind: StatusWidgetKind? = null,
    val needsPermission: Boolean = false
) {
    Battery(R.string.widget_battery, ShapeCatalog.SoftBurst, StatusWidgetKind.Battery),
    Cellular(R.string.widget_cellular, ShapeCatalog.Pentagon, StatusWidgetKind.Cellular),
    Wifi(R.string.widget_wifi, ShapeCatalog.Fan, StatusWidgetKind.Wifi),
    Sound(R.string.widget_sound, ShapeCatalog.Circle, StatusWidgetKind.Sound),
    Alarm(R.string.widget_alarm, ShapeCatalog.SemiCircle, StatusWidgetKind.Alarm),
    Time(R.string.widget_time, ShapeCatalog.Sunny, StatusWidgetKind.Time),
    Alerts(R.string.widget_alerts, ShapeCatalog.Clover4Leaf, StatusWidgetKind.Alerts, needsPermission = true),
    NowPlaying(
        R.string.widget_now_playing,
        ShapeCatalog.Cookie12Sided,
        StatusWidgetKind.NowPlaying,
        needsPermission = true
    ),
    Week(R.string.widget_week, ShapeCatalog.Diamond),
    Mochi(R.string.widget_mochi, ShapeCatalog.Puffy),
    Breathe(R.string.widget_breathe, ShapeCatalog.Flower),
    HeartPulse(R.string.widget_heart_pulse, ShapeCatalog.Heart, needsPermission = true),
    InnerCircle(R.string.widget_inner_circle, ShapeCatalog.Heart),
    Decide(R.string.widget_decide, ShapeCatalog.Cookie9Sided)
}

enum class WidgetCategory(
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int,
    val widgets: List<CatalogWidget>
) {
    Status(
        R.string.category_status,
        R.string.category_status_subtitle,
        listOf(
            CatalogWidget.Battery,
            CatalogWidget.Cellular,
            CatalogWidget.Wifi,
            CatalogWidget.Sound,
            CatalogWidget.Alarm,
            CatalogWidget.Time,
            CatalogWidget.Alerts,
            CatalogWidget.NowPlaying
        )
    ),
    Time(
        R.string.category_time,
        R.string.category_time_subtitle,
        listOf(CatalogWidget.Time, CatalogWidget.Alarm, CatalogWidget.Week)
    ),
    Companion(
        R.string.category_companion,
        R.string.category_companion_subtitle,
        listOf(CatalogWidget.Mochi, CatalogWidget.Breathe, CatalogWidget.HeartPulse)
    ),
    People(R.string.category_people, R.string.category_people_subtitle, listOf(CatalogWidget.InnerCircle)),
    Fun(R.string.category_fun, R.string.category_fun_subtitle, listOf(CatalogWidget.Decide))
}

/** Case-insensitive matches of [query] by name, in catalog order. A blank query matches nothing. */
fun <T> List<T>.matching(query: String, name: (T) -> String): List<T> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) emptyList() else filter { name(it).contains(trimmed, ignoreCase = true) }
}
