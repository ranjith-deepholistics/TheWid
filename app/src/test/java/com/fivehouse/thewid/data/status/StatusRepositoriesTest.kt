package com.fivehouse.thewid.data.status

import android.media.AudioManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeBatterySource(vararg values: BatteryReading) : BatterySource {
    override val readings = flowOf(*values)
}

private class FakeCellularSource(vararg values: CellularReading) : CellularSource {
    override val readings = flowOf(*values)
}

private class FakeWifiSource(vararg values: WifiReading) : WifiSource {
    override val readings = flowOf(*values)
}

private class FakeSoundSource(vararg values: Int) : SoundSource {
    override val ringerModes = flowOf(*values.toTypedArray())
}

private class FakeAlarmSource(vararg values: Long?) : AlarmSource {
    override val nextTriggerMillis = flowOf(*values)
}

private class FakeTimeSource(vararg values: ZonedDateTime) : TimeSource {
    override val now = flowOf(*values)
}

private fun <T> Flow<T>.collected(): List<T> = runBlocking { toList() }

class BatteryRepositoryTest {
    private fun statusOf(reading: BatteryReading) =
        BatteryRepository(FakeBatterySource(reading)).status.collected().single()

    private fun reading(level: Int = 68, scale: Int = 100, status: Int = BatteryManager.BATTERY_STATUS_DISCHARGING) =
        BatteryReading(level, scale, status)

    @Test
    fun percentIsScaledToTheReportedRange() {
        assertEquals(68, statusOf(reading()).percent)
        assertEquals(50, statusOf(reading(level = 100, scale = 200)).percent)
    }

    @Test
    fun missingOrInvalidLevelIsUnknown() {
        assertEquals(null, statusOf(reading(level = -1)).percent)
        assertEquals(null, statusOf(reading(scale = -1)).percent)
        assertEquals(null, statusOf(reading(scale = 0)).percent)
    }

    @Test
    fun chargingCoversChargingAndFullOnly() {
        assertTrue(statusOf(reading(status = BatteryManager.BATTERY_STATUS_CHARGING)).isCharging)
        assertTrue(statusOf(reading(status = BatteryManager.BATTERY_STATUS_FULL)).isCharging)
        assertFalse(statusOf(reading(status = BatteryManager.BATTERY_STATUS_DISCHARGING)).isCharging)
        assertFalse(statusOf(reading(status = BatteryManager.BATTERY_STATUS_NOT_CHARGING)).isCharging)
    }

    @Test
    fun repeatsOfTheSameStatusAreDropped() {
        val statuses = BatteryRepository(
            FakeBatterySource(reading(), reading(), reading(level = 67))
        ).status.collected()
        assertEquals(listOf(68, 67), statuses.map { it.percent })
    }
}

class CellularRepositoryTest {
    private fun reading(
        level: Int? = 3,
        networkType: Int = TelephonyManager.NETWORK_TYPE_LTE,
        override: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
    ) = CellularReading(level, networkType, override)

    private fun statusOf(reading: CellularReading) =
        CellularRepository(FakeCellularSource(reading)).status.collected().single()

    private fun networkOf(reading: CellularReading) = (statusOf(reading) as CellularStatus.Signal).network

    @Test
    fun levelsBecomeBarsAndNoReadingIsUnavailable() {
        assertEquals(CellularStatus.Unavailable, statusOf(reading(level = null)))
        assertEquals(CellularStatus.Signal(0, CellularNetwork.FourG), statusOf(reading(level = 0)))
        assertEquals(CellularStatus.Signal(4, CellularNetwork.FourG), statusOf(reading(level = 4)))
    }

    @Test
    fun barsStayInRange() {
        assertEquals(CellularStatus.Signal(MAX_SIGNAL_BARS, CellularNetwork.FourG), statusOf(reading(level = 9)))
    }

    @Test
    fun fiveGShowsOnStandaloneAndOnLteAnchoredNetworks() {
        assertEquals(CellularNetwork.FiveG, networkOf(reading(networkType = TelephonyManager.NETWORK_TYPE_NR)))
        assertEquals(
            CellularNetwork.FiveG,
            networkOf(reading(override = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA))
        )
        assertEquals(
            CellularNetwork.FiveG,
            networkOf(reading(override = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED))
        )
    }

    @Test
    fun lteOverridesThatAreNotFiveGStayFourG() {
        assertEquals(
            CellularNetwork.FourG,
            networkOf(reading(override = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA))
        )
    }

    @Test
    fun olderNetworksMapToTheirGeneration() {
        assertEquals(CellularNetwork.ThreeG, networkOf(reading(networkType = TelephonyManager.NETWORK_TYPE_HSPAP)))
        assertEquals(CellularNetwork.TwoG, networkOf(reading(networkType = TelephonyManager.NETWORK_TYPE_EDGE)))
    }

    @Test
    fun noMobileDataHasSignalButNoLabel() {
        assertEquals(null, networkOf(reading(networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN)))
        assertEquals(null, networkOf(reading(networkType = TelephonyManager.NETWORK_TYPE_IWLAN)))
    }
}

class WifiRepositoryTest {
    private fun statusFor(rssi: Int) =
        WifiRepository(FakeWifiSource(WifiReading.Connected(rssi))).status.collected().single()

    @Test
    fun disconnectedIsOff() {
        assertEquals(
            listOf(WifiStatus.Off),
            WifiRepository(FakeWifiSource(WifiReading.Disconnected)).status.collected()
        )
    }

    @Test
    fun strengthMapsAcrossTheRange() {
        assertEquals(WifiStatus.Connected(0), statusFor(-100))
        assertEquals(WifiStatus.Connected(0), statusFor(-120))
        assertEquals(WifiStatus.Connected(2), statusFor(-77))
        assertEquals(WifiStatus.Connected(4), statusFor(-55))
        assertEquals(WifiStatus.Connected(4), statusFor(-30))
    }

    @Test
    fun unreportedStrengthIsNotShownAsZeroBars() {
        assertEquals(WifiStatus.Connected(null), statusFor(NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED))
    }

    @Test
    fun readingsWithTheSameBarsAreDropped() {
        val statuses = WifiRepository(
            FakeWifiSource(WifiReading.Connected(-56), WifiReading.Connected(-55), WifiReading.Disconnected)
        ).status.collected()
        assertEquals(listOf(WifiStatus.Connected(3), WifiStatus.Connected(4), WifiStatus.Off), statuses)
    }
}

class SoundRepositoryTest {
    @Test
    fun ringerModesMapToStatuses() {
        val statuses = SoundRepository(
            FakeSoundSource(
                AudioManager.RINGER_MODE_NORMAL,
                AudioManager.RINGER_MODE_VIBRATE,
                AudioManager.RINGER_MODE_SILENT
            )
        ).status.collected()
        assertEquals(listOf(SoundStatus.Ring, SoundStatus.Vibrate, SoundStatus.Silent), statuses)
    }
}

class AlarmRepositoryTest {
    @Test
    fun triggerTimeBecomesScheduledAndNullBecomesNone() {
        val statuses = AlarmRepository(FakeAlarmSource(1_000L, null)).status.collected()
        assertEquals(listOf(AlarmStatus.Scheduled(Instant.ofEpochSecond(1)), AlarmStatus.None), statuses)
    }
}

class TimeRepositoryTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(2026, 11, 2, hour, minute, second, 0, zone)

    private fun isDayAt(hour: Int, minute: Int) =
        TimeRepository(FakeTimeSource(at(hour, minute))).status.collected().single().isDay

    @Test
    fun dayRunsFromSixUntilEighteen() {
        assertFalse(isDayAt(5, 59))
        assertTrue(isDayAt(6, 0))
        assertTrue(isDayAt(17, 59))
        assertFalse(isDayAt(18, 0))
    }

    @Test
    fun updatesWithinTheSameMinuteAreDropped() {
        val statuses = TimeRepository(FakeTimeSource(at(9, 42, 1), at(9, 42, 59), at(9, 43, 0))).status.collected()
        assertEquals(listOf(at(9, 42), at(9, 43)), statuses.map { it.dateTime })
    }
}
