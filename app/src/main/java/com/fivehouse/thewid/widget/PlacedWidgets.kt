package com.fivehouse.thewid.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PlacedWidget(
    val appWidgetId: Int,
    val kind: StatusWidgetKind,
    val layout: WidgetLayout,
    val shape: ShapeCatalog?,
    val glow: Boolean
)

@Singleton
class PlacedWidgets @Inject constructor(@param:ApplicationContext private val context: Context) {
    suspend fun load(): List<PlacedWidget> {
        val manager = AppWidgetManager.getInstance(context)
        val glance = GlanceAppWidgetManager(context)
        return StatusWidgetKind.entries.flatMap { kind ->
            manager.getAppWidgetIds(ComponentName(context, kind.receiver.java)).map { id ->
                val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glance.getGlanceIdBy(id))
                val options = manager.getAppWidgetOptions(id)
                val size = DpSize(
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).dp,
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).dp
                )
                PlacedWidget(id, kind, widgetLayoutFor(size), prefs.chosenShape(), prefs.glow())
            }
        }
    }
}
