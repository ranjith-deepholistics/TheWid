package com.fivehouse.thewid.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetPinning {
    /** When false, send people to the add-by-hand screen instead. */
    fun isSupported(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    fun request(context: Context, kind: StatusWidgetKind): Boolean =
        AppWidgetManager.getInstance(context).requestPinAppWidget(
            ComponentName(context, kind.receiver.java),
            null,
            null
        )
}
