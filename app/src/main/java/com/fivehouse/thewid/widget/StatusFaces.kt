package com.fivehouse.thewid.widget

import android.content.Context
import android.text.format.DateFormat
import com.fivehouse.thewid.data.personal.PersonalRepository
import com.fivehouse.thewid.data.personal.moodFor
import com.fivehouse.thewid.data.status.AlarmRepository
import com.fivehouse.thewid.data.status.AlertsRepository
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.data.status.BatteryRepository
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.CellularRepository
import com.fivehouse.thewid.data.status.NowPlayingRepository
import com.fivehouse.thewid.data.status.SoundRepository
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.data.status.TimeRepository
import com.fivehouse.thewid.data.status.WifiRepository
import com.fivehouse.thewid.data.status.WifiStatus
import com.fivehouse.thewid.data.status.platform.AndroidAlarmSource
import com.fivehouse.thewid.data.status.platform.AndroidAlertsSource
import com.fivehouse.thewid.data.status.platform.AndroidBatterySource
import com.fivehouse.thewid.data.status.platform.AndroidCellularSource
import com.fivehouse.thewid.data.status.platform.AndroidNowPlayingSource
import com.fivehouse.thewid.data.status.platform.AndroidSoundSource
import com.fivehouse.thewid.data.status.platform.AndroidTimeSource
import com.fivehouse.thewid.data.status.platform.AndroidWifiSource
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Turns live status into what a widget of each kind draws. */
@Singleton
class StatusFaces @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val battery: BatteryRepository,
    private val cellular: CellularRepository,
    private val wifi: WifiRepository,
    private val sound: SoundRepository,
    private val alarm: AlarmRepository,
    private val time: TimeRepository,
    private val alerts: AlertsRepository,
    private val nowPlaying: NowPlayingRepository,
    private val personal: PersonalRepository
) {
    /**
     * @param shape the user's choice. Ignored for kinds whose shape reports the state.
     * @param glow whether Battery turns accent while charging.
     */
    fun faces(kind: StatusWidgetKind, shape: ShapeCatalog? = null, glow: Boolean = true): Flow<StatusFace> {
        val chosen = shape?.takeIf { it in kind.shapeOptions } ?: kind.shapeOptions.firstOrNull()
        return when (kind) {
            StatusWidgetKind.Battery -> battery.status.map { it.toFace(checkNotNull(chosen), glow) }

            StatusWidgetKind.Cellular -> cellular.status.map { it.toFace(checkNotNull(chosen)) }

            StatusWidgetKind.Wifi -> wifi.status.map { it.toFace(checkNotNull(chosen)) }

            StatusWidgetKind.Sound -> sound.status.map { it.toFace() }

            StatusWidgetKind.Alarm -> alarm.status.map {
                it.toFace(ZoneId.systemDefault(), DateFormat.is24HourFormat(context), checkNotNull(chosen))
            }

            StatusWidgetKind.Time -> time.status.map { it.toFace() }

            StatusWidgetKind.Alerts -> alerts.status.map { it.toFace() }

            StatusWidgetKind.NowPlaying -> nowPlaying.status.map { it.toFace() }

            StatusWidgetKind.Week -> combine(personal.events, time.status) { events, now ->
                weekFace(events, now.dateTime.toLocalDate())
            }.distinctUntilChanged()

            StatusWidgetKind.InnerCircle -> personal.people.map(::peopleFace)

            StatusWidgetKind.Decide -> personal.decision.map { it.toFace() }

            StatusWidgetKind.Breathe -> combine(personal.breathe, time.status) { log, now ->
                log.toFace(now.dateTime.toLocalDate())
            }.distinctUntilChanged()

            StatusWidgetKind.Mochi -> mochiFaces()
        }
    }

    private fun mochiFaces(): Flow<StatusFace> {
        val phone = combine(battery.status, sound.status, wifi.status, alerts.status) { power, ringer, network, alert ->
            PhoneMood(power, ringer, network, alert)
        }
        return combine(phone, personal.mochi) { state, settings ->
            moodFor(
                settings = settings,
                isCharging = state.battery.isCharging,
                batteryPercent = state.battery.percent,
                isSilent = state.sound == SoundStatus.Silent,
                isWifiOff = state.wifi == WifiStatus.Off,
                alertCount = (state.alerts as? AlertsStatus.Count)?.count
            ).toFace(settings.name)
        }.distinctUntilChanged()
    }

    private data class PhoneMood(
        val battery: BatteryStatus,
        val sound: SoundStatus,
        val wifi: WifiStatus,
        val alerts: AlertsStatus
    )
}

@Module
@InstallIn(SingletonComponent::class)
object StatusModule {
    @Provides
    @Singleton
    fun battery(@ApplicationContext context: Context) = BatteryRepository(AndroidBatterySource(context))

    @Provides
    @Singleton
    fun cellular(@ApplicationContext context: Context) = CellularRepository(AndroidCellularSource(context))

    @Provides
    @Singleton
    fun wifi(@ApplicationContext context: Context) = WifiRepository(AndroidWifiSource(context))

    @Provides
    @Singleton
    fun sound(@ApplicationContext context: Context) = SoundRepository(AndroidSoundSource(context))

    @Provides
    @Singleton
    fun alarm(@ApplicationContext context: Context) = AlarmRepository(AndroidAlarmSource(context))

    @Provides
    @Singleton
    fun time(@ApplicationContext context: Context) = TimeRepository(AndroidTimeSource(context))

    @Provides
    @Singleton
    fun alerts() = AlertsRepository(AndroidAlertsSource())

    @Provides
    @Singleton
    fun nowPlaying(@ApplicationContext context: Context) = NowPlayingRepository(AndroidNowPlayingSource(context))
}

/** Widgets are created by the system, so they reach Hilt through this instead of injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StatusFacesEntryPoint {
    fun statusFaces(): StatusFaces

    fun personalRepository(): PersonalRepository
}

fun Context.statusFaces(): StatusFaces =
    EntryPointAccessors.fromApplication(applicationContext, StatusFacesEntryPoint::class.java).statusFaces()

fun Context.personalRepository(): PersonalRepository =
    EntryPointAccessors.fromApplication(applicationContext, StatusFacesEntryPoint::class.java).personalRepository()
