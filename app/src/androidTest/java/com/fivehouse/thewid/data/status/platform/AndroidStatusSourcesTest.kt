package com.fivehouse.thewid.data.status.platform

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fivehouse.thewid.data.status.AlarmRepository
import com.fivehouse.thewid.data.status.AlertsRepository
import com.fivehouse.thewid.data.status.BatteryRepository
import com.fivehouse.thewid.data.status.CellularRepository
import com.fivehouse.thewid.data.status.NowPlayingRepository
import com.fivehouse.thewid.data.status.SoundRepository
import com.fivehouse.thewid.data.status.TimeRepository
import com.fivehouse.thewid.data.status.WifiRepository
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs each real source on a device: it has to deliver a first value and unregister cleanly,
 * with no permission beyond what the manifest declares.
 */
@RunWith(AndroidJUnit4::class)
class AndroidStatusSourcesTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun <T> Flow<T>.firstValue(): T = runBlocking { withTimeout(5.seconds) { first() } }

    @Test
    fun batteryReportsAUsablePercent() {
        val percent = BatteryRepository(AndroidBatterySource(context)).status.firstValue().percent
        assertNotNull(percent)
        assertTrue("percent $percent", percent in 0..100)
    }

    @Test
    fun cellularDelivers() {
        assertNotNull(CellularRepository(AndroidCellularSource(context)).status.firstValue())
    }

    @Test
    fun wifiDelivers() {
        assertNotNull(WifiRepository(AndroidWifiSource(context)).status.firstValue())
    }

    @Test
    fun soundDelivers() {
        assertNotNull(SoundRepository(AndroidSoundSource(context)).status.firstValue())
    }

    @Test
    fun alarmDelivers() {
        assertNotNull(AlarmRepository(AndroidAlarmSource(context)).status.firstValue())
    }

    @Test
    fun alertsDeliversWithOrWithoutAccess() {
        NotificationAccess.refresh(context)
        assertNotNull(AlertsRepository(AndroidAlertsSource()).status.firstValue())
    }

    @Test
    fun nowPlayingDeliversWithOrWithoutAccess() {
        NotificationAccess.refresh(context)
        assertNotNull(NowPlayingRepository(AndroidNowPlayingSource(context)).status.firstValue())
    }

    @Test
    fun timeIsTheCurrentMinute() {
        val time = TimeRepository(AndroidTimeSource(context)).status.firstValue().dateTime
        val drift = Duration.between(time, ZonedDateTime.now()).abs()
        assertTrue("drift $drift", drift < Duration.ofMinutes(2))
    }
}
