package com.fivehouse.thewid.data.status

import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.widget.FaceValue
import com.fivehouse.thewid.widget.toFace
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAlertsSource(vararg values: Int?) : AlertsSource {
    override val counts = flowOf(*values)
}

private class FakeNowPlayingSource(vararg values: List<MediaSnapshot>?) : NowPlayingSource {
    override val sessions = flowOf(*values)
}

class NotificationStatusTest {
    private fun alerts(vararg values: Int?) =
        runBlocking { AlertsRepository(FakeAlertsSource(*values)).status.toList() }

    private fun playing(session: List<MediaSnapshot>?) =
        runBlocking { NowPlayingRepository(FakeNowPlayingSource(session)).status.toList() }.single()

    private fun snapshot(title: String, isPlaying: Boolean) = MediaSnapshot(title, null, isPlaying, null)

    @Test
    fun noAccessIsDistinctFromZeroAlerts() {
        assertEquals(listOf(AlertsStatus.NoAccess, AlertsStatus.Count(0), AlertsStatus.Count(2)), alerts(null, 0, 0, 2))
    }

    @Test
    fun onlyDismissibleNotificationsFromOtherAppsCount() {
        assertTrue(countsAsAlert(isOngoing = false, isClearable = true, isGroupSummary = false, isOwn = false))
        assertFalse(countsAsAlert(isOngoing = true, isClearable = true, isGroupSummary = false, isOwn = false))
        assertFalse(countsAsAlert(isOngoing = false, isClearable = false, isGroupSummary = false, isOwn = false))
        assertFalse(countsAsAlert(isOngoing = false, isClearable = true, isGroupSummary = true, isOwn = false))
        assertFalse(countsAsAlert(isOngoing = false, isClearable = true, isGroupSummary = false, isOwn = true))
    }

    @Test
    fun alertsCountGoesInsideTheClover() {
        val face = AlertsStatus.Count(3).toFace()
        assertEquals(ShapeCatalog.Clover4Leaf, face.shape)
        assertEquals(FaceValue.Literal("3"), face.value)
        assertTrue(face.valueInside)
    }

    @Test
    fun noAccessIsDashedAndFlagged() {
        val alertsFace = AlertsStatus.NoAccess.toFace()
        assertEquals(ShapeSurface.Dashed, alertsFace.surface)
        assertTrue(alertsFace.needsAccess)
        assertTrue(NowPlayingStatus.NoAccess.toFace().needsAccess)
    }

    @Test
    fun sessionsMapToNoAccessNothingOrATrack() {
        assertEquals(NowPlayingStatus.NoAccess, playing(null))
        assertEquals(NowPlayingStatus.Nothing, playing(emptyList()))
        assertEquals(
            NowPlayingStatus.Track("A", null, false, null),
            playing(listOf(snapshot("A", isPlaying = false)))
        )
    }

    @Test
    fun aPlayingSessionWinsOverAnEarlierPausedOne() {
        val status = playing(listOf(snapshot("Paused", false), snapshot("Playing", true)))
        assertEquals(NowPlayingStatus.Track("Playing", null, true, null), status)
    }

    @Test
    fun cookieWhilePlayingSquareWhenPausedDashedWhenNothing() {
        assertEquals(ShapeCatalog.Cookie12Sided, NowPlayingStatus.Track("A", null, true, null).toFace().shape)
        assertEquals(ShapeCatalog.Square, NowPlayingStatus.Track("A", null, false, null).toFace().shape)
        assertEquals(ShapeSurface.Dashed, NowPlayingStatus.Nothing.toFace().surface)
    }

    @Test
    fun trackFaceCaptionsTheTitleAndIsLiveOnlyWhilePlaying() {
        val face = NowPlayingStatus.Track("Song", "Artist", true, null).toFace()
        assertEquals("Song", face.caption)
        assertTrue(face.isLive)
        assertFalse(NowPlayingStatus.Track("Song", "Artist", false, null).toFace().isLive)
    }
}
