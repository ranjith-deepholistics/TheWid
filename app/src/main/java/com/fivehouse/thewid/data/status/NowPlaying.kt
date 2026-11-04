package com.fivehouse.thewid.data.status

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface NowPlayingStatus {
    data object NoAccess : NowPlayingStatus

    /** Access is on but no app has a media session. */
    data object Nothing : NowPlayingStatus

    /** @property leftMs time left in the track, or null when the app doesn't report it. */
    data class Track(
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val art: Bitmap?,
        val leftMs: Long? = null,
        val durationMs: Long? = null
    ) : NowPlayingStatus
}

/** One app's media session. */
data class MediaSnapshot(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val art: Bitmap?,
    val leftMs: Long? = null,
    val durationMs: Long? = null
)

interface NowPlayingSource {
    /** Every active media session, or null without notification access. */
    val sessions: Flow<List<MediaSnapshot>?>
}

class NowPlayingRepository(source: NowPlayingSource) {
    val status: Flow<NowPlayingStatus> = source.sessions
        .map { sessions ->
            when {
                sessions == null -> NowPlayingStatus.NoAccess

                sessions.isEmpty() -> NowPlayingStatus.Nothing

                else -> {
                    // The session list is ordered by priority, so without a playing one take the first.
                    val chosen = sessions.firstOrNull { it.isPlaying } ?: sessions.first()
                    NowPlayingStatus.Track(
                        chosen.title,
                        chosen.artist,
                        chosen.isPlaying,
                        chosen.art,
                        chosen.leftMs,
                        chosen.durationMs
                    )
                }
            }
        }
        .distinctUntilChanged()
}
