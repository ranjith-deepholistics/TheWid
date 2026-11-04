package com.fivehouse.thewid

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fivehouse.thewid.data.status.platform.NotificationAccess
import com.fivehouse.thewid.widget.ExactMinute
import com.fivehouse.thewid.widget.WidgetPreviews
import com.fivehouse.thewid.widget.WidgetRefresh
import com.fivehouse.thewid.widget.personalRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class ShapesApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationAccess.refresh(this)
        WidgetRefresh.schedulePeriodic(this)
        WidgetRefresh.listenForChanges(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            if (personalRepository().timeSettings.first().exactMinute) ExactMinute.scheduleNext(this@ShapesApplication)
            WidgetPreviews.publish(this@ShapesApplication)
        }
    }
}
