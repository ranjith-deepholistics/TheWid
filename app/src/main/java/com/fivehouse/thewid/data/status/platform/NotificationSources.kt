package com.fivehouse.thewid.data.status.platform

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.scale
import androidx.glance.appwidget.updateAll
import com.fivehouse.thewid.data.status.AlertsSource
import com.fivehouse.thewid.data.status.MediaSnapshot
import com.fivehouse.thewid.data.status.NowPlayingSource
import com.fivehouse.thewid.data.status.countsAsAlert
import com.fivehouse.thewid.heartpulse.HeartPulse
import com.fivehouse.thewid.heartpulse.PulseTrigger
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.statusFaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

private const val MAX_ART_PX = 256
private const val WIDGET_UPDATE_DEBOUNCE_MS = 1_000L

/** Whether the user has granted notification access. Refresh after anything that could change it. */
object NotificationAccess {
    private val state = MutableStateFlow(false)
    val granted: StateFlow<Boolean> = state.asStateFlow()

    fun refresh(context: Context) {
        state.value = context.getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(ComponentName(context, ShapesNotificationListener::class.java))
    }
}

/**
 * Keeps a count, plus the sender name of message notifications to match against the Inner Circle.
 * Names stay in memory only. This must never read a notification's text: the permission screen
 * promises that.
 */
class ShapesNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onListenerConnected() {
        NotificationAccess.refresh(this)
        recount()
        updateWidgetsOnChange(StatusWidgetKind.Alerts)
        updateWidgetsOnChange(StatusWidgetKind.NowPlaying)
    }

    override fun onListenerDisconnected() {
        NotificationAccess.refresh(this)
        scope.coroutineContext.cancelChildren()
        count.value = null
        messageSenders.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        recount()
        val counts = countsAsAlert(
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,
            isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOwn = sbn.packageName == packageName
        )
        if (counts) HeartPulse.trigger(this, PulseTrigger.Notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = recount()

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        count.value = null
        super.onDestroy()
    }

    private fun recount() {
        val active = try {
            activeNotifications
        } catch (_: SecurityException) {
            null // Access was revoked or the listener isn't bound yet.
        }
        messageSenders.value = active.orEmpty()
            .filter { it.notification.category == Notification.CATEGORY_MESSAGE }
            .mapNotNull { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() }
        count.value = active?.count { sbn ->
            countsAsAlert(
                isOngoing = sbn.isOngoing,
                isClearable = sbn.isClearable,
                isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                isOwn = sbn.packageName == packageName
            )
        }
    }

    /** Waits for a quiet second so a burst of notifications redraws once. */
    private fun updateWidgetsOnChange(kind: StatusWidgetKind) {
        scope.launch {
            statusFaces().faces(kind).collectLatest {
                delay(WIDGET_UPDATE_DEBOUNCE_MS)
                kind.newWidget().updateAll(this@ShapesNotificationListener)
            }
        }
    }

    companion object {
        private val count = MutableStateFlow<Int?>(null)
        private val messageSenders = MutableStateFlow<List<String>>(emptyList())

        /** Who waiting messages are from, for the Inner Circle's "new messages". */
        val senders: StateFlow<List<String>> = messageSenders.asStateFlow()

        /** The current count, or null while the listener isn't connected. */
        val alertCount: StateFlow<Int?> = count.asStateFlow()
    }
}

class AndroidAlertsSource : AlertsSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val counts: Flow<Int?> = NotificationAccess.granted.flatMapLatest { granted ->
        if (granted) ShapesNotificationListener.alertCount else flowOf(null)
    }
}

class AndroidNowPlayingSource(context: Context) : NowPlayingSource {
    private val appContext = context.applicationContext

    @OptIn(ExperimentalCoroutinesApi::class)
    override val sessions: Flow<List<MediaSnapshot>?> = NotificationAccess.granted.flatMapLatest { granted ->
        if (granted) liveSessions() else flowOf(null)
    }

    private fun liveSessions(): Flow<List<MediaSnapshot>?> = callbackFlow {
        val manager = appContext.getSystemService(MediaSessionManager::class.java)
        val listenerComponent = ComponentName(appContext, ShapesNotificationListener::class.java)
        val handler = Handler(Looper.getMainLooper())
        var controllers = emptyList<MediaController>()
        val publish = { trySend(controllers.map { it.toSnapshot() }) }
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                publish()
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                publish()
            }
        }
        val watch = { active: List<MediaController>? ->
            controllers.forEach { it.unregisterCallback(callback) }
            controllers = active.orEmpty()
            controllers.forEach { it.registerCallback(callback, handler) }
            publish()
        }
        val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { watch(it) }
        val registered = try {
            watch(manager.getActiveSessions(listenerComponent))
            manager.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent, handler)
            true
        } catch (_: SecurityException) {
            // Access was revoked between the check and this call.
            trySend(null)
            false
        }
        awaitClose {
            controllers.forEach { it.unregisterCallback(callback) }
            if (registered) manager.removeOnActiveSessionsChangedListener(sessionsChanged)
        }
    }.flowOn(Dispatchers.Main.immediate)

    private fun MediaController.toSnapshot(): MediaSnapshot {
        val metadata = metadata
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 }
        val position = playbackState?.position?.takeIf { it >= 0 }
        return MediaSnapshot(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING ||
                playbackState?.state == PlaybackState.STATE_BUFFERING,
            art = (
                metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                )?.bounded(),
            leftMs = if (duration != null && position != null) (duration - position).coerceAtLeast(0) else null,
            durationMs = duration
        )
    }

    /** Apps can hand over very large art, which would overflow a widget update. */
    private fun Bitmap.bounded(): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= MAX_ART_PX) return this
        return scale(width * MAX_ART_PX / longest, height * MAX_ART_PX / longest)
    }
}

/** Play, pause and skip for whichever media session is showing. */
class MediaTransport(context: Context) {
    private val appContext = context.applicationContext

    private fun active(): MediaController? = try {
        val sessions = appContext.getSystemService(MediaSessionManager::class.java)
            .getActiveSessions(ComponentName(appContext, ShapesNotificationListener::class.java))
        sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
    } catch (_: SecurityException) {
        null
    }

    fun playPause() {
        val controller = active() ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        active()?.transportControls?.skipToNext()
    }

    fun previous() {
        active()?.transportControls?.skipToPrevious()
    }
}
