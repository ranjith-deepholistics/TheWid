package com.fivehouse.thewid.data.status

import android.media.AudioManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class SoundStatus { Ring, Vibrate, Silent }

interface SoundSource {
    /** `AudioManager.getRingerMode()` values. */
    val ringerModes: Flow<Int>
}

class SoundRepository(source: SoundSource) {
    val status: Flow<SoundStatus> = source.ringerModes
        .map { mode ->
            when (mode) {
                AudioManager.RINGER_MODE_SILENT -> SoundStatus.Silent
                AudioManager.RINGER_MODE_VIBRATE -> SoundStatus.Vibrate
                else -> SoundStatus.Ring
            }
        }
        .distinctUntilChanged()
}
