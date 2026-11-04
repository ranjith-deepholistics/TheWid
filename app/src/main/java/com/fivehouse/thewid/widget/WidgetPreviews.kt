package com.fivehouse.thewid.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager

/** Publishes the launcher picker's previews. Below Android 15 launchers show the app icon instead. */
object WidgetPreviews {
    // Android rate-limits this, so a refused update is simply retried at the next app start.
    @SuppressLint("CheckResult")
    suspend fun publish(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val manager = GlanceAppWidgetManager(context)
        StatusWidgetKind.entries.forEach { kind -> manager.setWidgetPreviews(kind.receiver) }
    }
}
