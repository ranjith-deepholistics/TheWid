package com.fivehouse.thewid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val EXTRA_KIND = "kind"
private const val EXTRA_SHAPE = "shape"
private const val EXTRA_GLOW = "glow"

object WidgetPinning {
    private val lastPinned = MutableStateFlow<StatusWidgetKind?>(null)

    /** The kind most recently placed from the app, until [consumePinned] clears it. */
    val pinned: StateFlow<StatusWidgetKind?> = lastPinned.asStateFlow()

    fun consumePinned() {
        lastPinned.value = null
    }

    internal fun markPinned(kind: StatusWidgetKind) {
        lastPinned.value = kind
    }

    /** When false, send people to the add-by-hand screen instead. */
    fun isSupported(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** The launcher chooses the placed size. People resize from there. */
    fun request(context: Context, kind: StatusWidgetKind, shape: ShapeCatalog?, glow: Boolean): Boolean {
        val callback = Intent(context, WidgetPinnedReceiver::class.java)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_SHAPE, shape?.name)
            .putExtra(EXTRA_GLOW, glow)
        // Mutable so the launcher can add the new widget's id. The explicit component keeps it private.
        val pending = PendingIntent.getBroadcast(
            context,
            kind.ordinal,
            callback,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return AppWidgetManager.getInstance(context).requestPinAppWidget(
            ComponentName(context, kind.receiver.java),
            null,
            pending
        )
    }
}

/** Applies the choices made in Customize once the launcher has placed the widget. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = StatusWidgetKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) }
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || kind == null) return
        val shape = intent.getStringExtra(EXTRA_SHAPE)
        val glow = intent.getBooleanExtra(EXTRA_GLOW, true)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
                updateAppWidgetState(context, glanceId) { prefs ->
                    if (shape != null) prefs[WidgetShapeKey] = shape
                    prefs[WidgetGlowKey] = glow
                }
                kind.newWidget().update(context, glanceId)
                WidgetPinning.markPinned(kind)
            } finally {
                pending.finish()
            }
        }
    }
}
