package com.fivehouse.thewid.data.status

import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The top of the range for both Wi-Fi and cellular bars. */
const val MAX_SIGNAL_BARS = 4

// The range Android's own Wi-Fi level calculation has long used.
private const val WEAKEST_RSSI_DBM = -100
private const val STRONGEST_RSSI_DBM = -55

sealed interface WifiStatus {
    /** Not on Wi-Fi. This also covers Wi-Fi being on with no network joined. */
    data object Off : WifiStatus

    /** @property bars from 0 to [MAX_SIGNAL_BARS], or null if the system didn't report a strength. */
    data class Connected(val bars: Int?) : WifiStatus
}

sealed interface WifiReading {
    data object Disconnected : WifiReading

    /** @property rssiDbm `NetworkCapabilities.getSignalStrength()`. */
    data class Connected(val rssiDbm: Int) : WifiReading
}

interface WifiSource {
    val readings: Flow<WifiReading>
}

class WifiRepository(source: WifiSource) {
    val status: Flow<WifiStatus> = source.readings
        .map { reading ->
            when (reading) {
                WifiReading.Disconnected -> WifiStatus.Off
                is WifiReading.Connected -> WifiStatus.Connected(barsFor(reading.rssiDbm))
            }
        }
        .distinctUntilChanged()
}

private fun barsFor(rssiDbm: Int): Int? = when {
    rssiDbm == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> null
    rssiDbm <= WEAKEST_RSSI_DBM -> 0
    rssiDbm >= STRONGEST_RSSI_DBM -> MAX_SIGNAL_BARS
    else -> (rssiDbm - WEAKEST_RSSI_DBM) * MAX_SIGNAL_BARS / (STRONGEST_RSSI_DBM - WEAKEST_RSSI_DBM)
}
