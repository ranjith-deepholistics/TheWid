package com.fivehouse.thewid.data.status

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class CellularNetwork { TwoG, ThreeG, FourG, FiveG }

sealed interface CellularStatus {
    /** No radio, or the system has no signal reading. */
    data object Unavailable : CellularStatus

    /**
     * @property bars from 0 to [MAX_SIGNAL_BARS].
     * @property network the generation the status bar would show, or null when not on mobile data.
     */
    data class Signal(val bars: Int, val network: CellularNetwork?) : CellularStatus
}

/**
 * @property level `SignalStrength.getLevel()`, or null when there's no reading.
 * @property networkType a `TelephonyManager.NETWORK_TYPE_*` value.
 * @property overrideNetworkType a `TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_*` value.
 */
data class CellularReading(val level: Int?, val networkType: Int, val overrideNetworkType: Int)

interface CellularSource {
    val readings: Flow<CellularReading>
}

class CellularRepository(source: CellularSource) {
    val status: Flow<CellularStatus> = source.readings
        .map { reading ->
            if (reading.level == null) {
                CellularStatus.Unavailable
            } else {
                CellularStatus.Signal(reading.level.coerceIn(0, MAX_SIGNAL_BARS), reading.toNetwork())
            }
        }
        .distinctUntilChanged()
}

// Much of 5G attaches to an LTE anchor and reports its type as LTE, so the override decides that case.
private fun CellularReading.toNetwork(): CellularNetwork? = when (overrideNetworkType) {
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
    -> CellularNetwork.FiveG

    else -> when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> CellularNetwork.FiveG

        TelephonyManager.NETWORK_TYPE_LTE -> CellularNetwork.FourG

        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA
        -> CellularNetwork.ThreeG

        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GSM
        -> CellularNetwork.TwoG

        else -> null
    }
}
