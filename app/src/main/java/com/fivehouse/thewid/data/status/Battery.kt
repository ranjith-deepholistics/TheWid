package com.fivehouse.thewid.data.status

import android.os.BatteryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** @property percent charge from 0 to 100, or null if the system didn't report a usable level. */
data class BatteryStatus(val percent: Int?, val isCharging: Boolean)

/** Raw values from the battery broadcast. [chargeStatus] is a `BatteryManager.BATTERY_STATUS_*` value. */
data class BatteryReading(val level: Int, val scale: Int, val chargeStatus: Int)

interface BatterySource {
    val readings: Flow<BatteryReading>
}

class BatteryRepository(source: BatterySource) {
    val status: Flow<BatteryStatus> = source.readings.map { it.toStatus() }.distinctUntilChanged()
}

private fun BatteryReading.toStatus() = BatteryStatus(
    percent = if (level < 0 || scale <= 0) null else (level * 100 / scale).coerceIn(0, 100),
    isCharging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
        chargeStatus == BatteryManager.BATTERY_STATUS_FULL
)
