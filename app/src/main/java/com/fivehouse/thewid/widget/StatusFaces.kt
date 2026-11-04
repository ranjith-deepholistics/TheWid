package com.fivehouse.thewid.widget

import android.content.Context
import android.text.format.DateFormat
import com.fivehouse.thewid.data.status.AlarmRepository
import com.fivehouse.thewid.data.status.BatteryRepository
import com.fivehouse.thewid.data.status.CellularRepository
import com.fivehouse.thewid.data.status.SoundRepository
import com.fivehouse.thewid.data.status.TimeRepository
import com.fivehouse.thewid.data.status.WifiRepository
import com.fivehouse.thewid.data.status.platform.AndroidAlarmSource
import com.fivehouse.thewid.data.status.platform.AndroidBatterySource
import com.fivehouse.thewid.data.status.platform.AndroidCellularSource
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
    private val time: TimeRepository
) {
    /** @param shape the user's choice. Ignored for kinds whose shape reports the state. */
    fun faces(kind: StatusWidgetKind, shape: ShapeCatalog?): Flow<StatusFace> {
        val chosen = shape?.takeIf { it in kind.shapeOptions } ?: kind.shapeOptions.firstOrNull()
        return when (kind) {
            StatusWidgetKind.Battery -> battery.status.map { it.toFace(checkNotNull(chosen)) }

            StatusWidgetKind.Cellular -> cellular.status.map { it.toFace(checkNotNull(chosen)) }

            StatusWidgetKind.Wifi -> wifi.status.map { it.toFace(checkNotNull(chosen)) }

            StatusWidgetKind.Sound -> sound.status.map { it.toFace() }

            StatusWidgetKind.Alarm -> alarm.status.map {
                it.toFace(ZoneId.systemDefault(), DateFormat.is24HourFormat(context), checkNotNull(chosen))
            }

            StatusWidgetKind.Time -> time.status.map { it.toFace() }
        }
    }
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
}

/** Widgets are created by the system, so they reach Hilt through this instead of injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StatusFacesEntryPoint {
    fun statusFaces(): StatusFaces
}

fun Context.statusFaces(): StatusFaces =
    EntryPointAccessors.fromApplication(applicationContext, StatusFacesEntryPoint::class.java).statusFaces()
