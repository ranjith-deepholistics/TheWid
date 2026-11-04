package com.fivehouse.thewid.widget

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fivehouse.thewid.heartpulse.HeartPulse
import com.fivehouse.thewid.heartpulse.PulseTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

private const val PERIODIC_WORK = "widget_refresh_periodic"
private const val IMMEDIATE_WORK = "widget_refresh_now"
private const val DELAYED_WORK = "widget_refresh_later"
private const val REFRESH_INTERVAL_MINUTES = 15L

private val RuntimeOnlyActions = listOf(
    Intent.ACTION_POWER_CONNECTED,
    Intent.ACTION_POWER_DISCONNECTED,
    AudioManager.RINGER_MODE_CHANGED_ACTION
)
private val RefreshActions = RuntimeOnlyActions + listOf(
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED
)

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        StatusWidgetKind.entries.forEach { kind -> kind.newWidget().updateAll(applicationContext) }
        // Countdowns run in the launcher, so redraw when a focus phase ends.
        val nowMs = System.currentTimeMillis()
        applicationContext.personalRepository().focus.first().settledAt(nowMs).endsAtEpochMs?.let { end ->
            WidgetRefresh.refreshAfter(applicationContext, end - nowMs)
        }
        return Result.success()
    }
}

object WidgetRefresh {
    fun schedulePeriodic(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        )
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        )
    }

    fun refreshAfter(context: Context, delayMs: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DELAYED_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .build()
        )
    }

    /**
     * Refreshes on broadcasts the manifest can't receive on current Android versions. These only
     * arrive while the app's process is alive; otherwise the periodic refresh catches the change.
     */
    fun listenForChanges(context: Context) {
        val filter = IntentFilter().apply { RuntimeOnlyActions.forEach(::addAction) }
        context.applicationContext.registerReceiver(StatusChangeReceiver(), filter, Context.RECEIVER_NOT_EXPORTED)
    }
}

/** Also declared in the manifest for the time, time zone and alarm broadcasts Android still delivers there. */
class StatusChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RefreshActions) WidgetRefresh.refreshNow(context)
        if (intent.action == Intent.ACTION_POWER_CONNECTED) HeartPulse.trigger(context, PulseTrigger.Charging)
    }
}
