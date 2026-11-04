package com.fivehouse.thewid.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MINUTE_MS = 60_000L

/**
 * Keeps the Time widget's minute marker exact. Uses a non-waking alarm, so it only fires while the
 * phone is awake and someone could be looking, rather than draining the battery overnight.
 */
object ExactMinute {
    fun canSchedule(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, MinuteTickReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun scheduleNext(context: Context) {
        if (!canSchedule(context)) return
        val next = (System.currentTimeMillis() / MINUTE_MS + 1) * MINUTE_MS
        context.getSystemService(AlarmManager::class.java).setExact(AlarmManager.RTC, next, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }
}

class MinuteTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (context.personalRepository().timeSettings.first().exactMinute) {
                    TimeWidget().updateAll(context)
                    ExactMinute.scheduleNext(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
