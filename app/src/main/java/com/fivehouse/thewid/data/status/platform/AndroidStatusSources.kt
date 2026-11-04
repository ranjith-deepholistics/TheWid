package com.fivehouse.thewid.data.status.platform

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.fivehouse.thewid.data.status.AlarmSource
import com.fivehouse.thewid.data.status.BatteryReading
import com.fivehouse.thewid.data.status.BatterySource
import com.fivehouse.thewid.data.status.CellularReading
import com.fivehouse.thewid.data.status.CellularSource
import com.fivehouse.thewid.data.status.SoundSource
import com.fivehouse.thewid.data.status.TimeSource
import com.fivehouse.thewid.data.status.WifiReading
import com.fivehouse.thewid.data.status.WifiSource
import java.time.ZonedDateTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Emits once on collection (the sticky intent if there is one, otherwise null), then every
 * matching broadcast. The receiver is registered only while the flow is collected, which
 * is what lets these system broadcasts reach the app on current Android versions.
 */
private fun Context.broadcasts(vararg actions: String): Flow<Intent?> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    val filter = IntentFilter().apply { actions.forEach(::addAction) }
    trySend(registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED))
    awaitClose { unregisterReceiver(receiver) }
}

class AndroidBatterySource(context: Context) : BatterySource {
    override val readings: Flow<BatteryReading> = context.applicationContext
        .broadcasts(Intent.ACTION_BATTERY_CHANGED)
        .filterNotNull()
        .map { intent ->
            BatteryReading(
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
                chargeStatus = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )
            )
        }
}

class AndroidCellularSource(context: Context) : CellularSource {
    private val appContext = context.applicationContext

    override val readings: Flow<CellularReading>
        get() {
            val hasRadio = appContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS)
            if (!hasRadio) {
                val noRadio = CellularReading(
                    level = null,
                    networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    overrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
                )
                return flowOf(noRadio)
            }
            val telephony = appContext.getSystemService(TelephonyManager::class.java)
            return combine(levels(telephony), displayedNetwork(telephony)) { level, network ->
                CellularReading(level, network.first, network.second)
            }
        }

    private fun levels(telephony: TelephonyManager): Flow<Int?> = callbackFlow {
        val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                trySend(signalStrength.level)
            }
        }
        trySend(telephony.signalStrength?.level)
        telephony.registerTelephonyCallback(appContext.mainExecutor, callback)
        awaitClose { telephony.unregisterTelephonyCallback(callback) }
    }

    /**
     * Network type paired with its display override. This callback needs no permission, unlike
     * `TelephonyManager.getDataNetworkType()`, so it starts from unknown until the first delivery.
     */
    private fun displayedNetwork(telephony: TelephonyManager): Flow<Pair<Int, Int>> = callbackFlow {
        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                trySend(telephonyDisplayInfo.networkType to telephonyDisplayInfo.overrideNetworkType)
            }
        }
        trySend(TelephonyManager.NETWORK_TYPE_UNKNOWN to TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE)
        telephony.registerTelephonyCallback(appContext.mainExecutor, callback)
        awaitClose { telephony.unregisterTelephonyCallback(callback) }
    }
}

/** Reads strength only. Without location permission the system redacts the network's name and details. */
class AndroidWifiSource(context: Context) : WifiSource {
    private val appContext = context.applicationContext

    override val readings: Flow<WifiReading> = callbackFlow {
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var current: Network? = null

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                current = network
                trySend(WifiReading.Connected(capabilities.signalStrength))
            }

            override fun onLost(network: Network) {
                if (network == current) {
                    current = null
                    trySend(WifiReading.Disconnected)
                }
            }
        }
        // The callback stays silent when no Wi-Fi network exists, so start from disconnected.
        trySend(WifiReading.Disconnected)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivity.registerNetworkCallback(request, callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
}

class AndroidSoundSource(context: Context) : SoundSource {
    private val appContext = context.applicationContext

    override val ringerModes: Flow<Int> = appContext
        .broadcasts(AudioManager.RINGER_MODE_CHANGED_ACTION)
        .map { appContext.getSystemService(AudioManager::class.java).ringerMode }
}

class AndroidAlarmSource(context: Context) : AlarmSource {
    private val appContext = context.applicationContext

    override val nextTriggerMillis: Flow<Long?> = appContext
        .broadcasts(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        .map { appContext.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime }
}

class AndroidTimeSource(context: Context) : TimeSource {
    override val now: Flow<ZonedDateTime> = context.applicationContext
        .broadcasts(Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
        .map { ZonedDateTime.now() }
}
