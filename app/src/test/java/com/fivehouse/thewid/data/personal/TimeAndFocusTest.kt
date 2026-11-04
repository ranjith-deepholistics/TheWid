package com.fivehouse.thewid.data.personal

import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.ui.shapes.ClockMarker
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.widget.FaceValue
import com.fivehouse.thewid.widget.formatCountdown
import com.fivehouse.thewid.widget.toFace
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeAndFocusTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")

    @Test
    fun sunTimesAreCloseToAlmanacValuesForChennai() {
        // Chennai on 21 March: sunrise and sunset close to 06:15 and 18:20 local time.
        val sun = assertNotNullSun(LocalDate.of(2026, 3, 21), 13.08, 80.27, kolkata)
        assertWithinMinutes(ZonedDateTime.parse("2026-03-21T06:15:00+05:30"), sun.sunrise, 15)
        assertWithinMinutes(ZonedDateTime.parse("2026-03-21T18:20:00+05:30"), sun.sunset, 15)
    }

    @Test
    fun sunriseAndSunsetFallOnTheRequestedLocalDayEverywhere() {
        val date = LocalDate.of(2026, 9, 10)
        listOf(
            Triple(35.68, 139.69, "Asia/Tokyo"),
            Triple(-33.87, 151.21, "Australia/Sydney"),
            Triple(40.71, -74.0, "America/New_York"),
            Triple(21.3, -157.86, "Pacific/Honolulu"),
            Triple(51.5, -0.12, "Europe/London")
        ).forEach { (lat, lon, zone) ->
            val sun = assertNotNullSun(date, lat, lon, ZoneId.of(zone))
            assertEquals(zone, date, sun.sunrise.toLocalDate())
            assertEquals(zone, date, sun.sunset.toLocalDate())
            assertTrue(zone, sun.sunrise.isBefore(sun.sunset))
        }
    }

    @Test
    fun polarSummerHasNoSunset() {
        assertNull(sunTimes(LocalDate.of(2026, 6, 21), 78.2, 15.6, ZoneId.of("UTC")))
    }

    private fun assertNotNullSun(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): SunTimes {
        val sun = sunTimes(date, lat, lon, zone)
        assertNotNull(sun)
        return sun!!
    }

    private fun assertWithinMinutes(expected: ZonedDateTime, actual: ZonedDateTime, minutes: Long) {
        val off = Duration.between(expected, actual).abs().toMinutes()
        assertTrue("$actual is $off minutes from $expected", off <= minutes)
    }

    private val noon = ZonedDateTime.parse("2026-11-02T12:00:00+05:30")
    private val sun = SunTimes(
        ZonedDateTime.parse("2026-11-02T06:10:00+05:30"),
        ZonedDateTime.parse("2026-11-02T18:00:00+05:30")
    )

    @Test
    fun timeFaceFollowsTheSunWhenItHasIt() {
        val evening = TimeStatus(ZonedDateTime.parse("2026-11-02T18:30:00+05:30"), isDay = true)
        assertEquals(ShapeCatalog.Cookie12Sided, evening.toFace(sun = sun).shape)
        assertEquals(ShapeCatalog.Sunny, TimeStatus(noon, isDay = false).toFace(sun = sun).shape)
    }

    @Test
    fun timeFaceCountsDownToSunset() {
        val face = TimeStatus(noon, isDay = true).toFace(sun = sun)
        assertEquals(listOf(6, 0), (face.footnote as FaceValue.Format).args)
    }

    @Test
    fun timeSettingsChangeTheFace() {
        val status = TimeStatus(noon, isDay = true)
        assertEquals(FaceValue.LiveClockSeconds, status.toFace(TimeSettings(seconds = true)).value)
        assertEquals(ClockMarker.Circle, status.toFace(TimeSettings(marker = MinuteMarker.Circle)).clock?.marker)
        assertEquals(ClockMarker.None, status.toFace(TimeSettings(marker = MinuteMarker.None)).clock?.marker)
        assertNull(status.toFace(TimeSettings(showDayTrack = false)).track)
        assertEquals(0.5f, status.toFace().track!!, 0.01f)
    }

    @Test
    fun messagesMatchNamesIgnoringCaseAndOnlyForThosePeople() {
        val ann = Person("a", "Ann", ShapeCatalog.Heart)
        val bo = Person("b", "Bo", ShapeCatalog.Flower)
        val counts = messagesPerPerson(listOf(ann, bo), listOf("ann ", "ANN", "Someone else"))
        assertEquals(2, counts[ann])
        assertEquals(0, counts[bo])
    }

    @Test
    fun calendarDatesJoinTheHandEnteredOnes() {
        val mine = WeekEvent("1", 5, "Mine", DayKind.Trip)
        val busy = WeekEvent("2", 6, "Meeting", DayKind.Busy)
        assertEquals(listOf(mine, busy), withCalendar(listOf(mine), listOf(busy)))
    }

    @Test
    fun focusCountsDownFromTheClockAndPausesInPlace() {
        val running = FocusState().started(1_000)
        assertTrue(running.isRunning)
        assertEquals(FOCUS_WORK_MS - 60_000, running.remainingAt(61_000))
        val paused = running.paused(61_000)
        assertFalse(paused.isRunning)
        assertEquals(FOCUS_WORK_MS - 60_000, paused.remainingAt(999_999))
    }

    @Test
    fun focusMovesToABreakThenTheNextSessionWhileClosed() {
        val running = FocusState().started(0)
        val onBreak = running.settledAt(FOCUS_WORK_MS)
        assertTrue(onBreak.onBreak)
        assertEquals(1, onBreak.session)
        assertTrue(onBreak.isRunning)
        val second = running.settledAt(FOCUS_WORK_MS + FOCUS_BREAK_MS)
        assertFalse(second.onBreak)
        assertEquals(2, second.session)
    }

    @Test
    fun focusStopsAfterTheLastSession() {
        val last = FocusState(session = FOCUS_SESSIONS).started(0)
        assertEquals(FocusState(), last.settledAt(FOCUS_WORK_MS + 1))
    }

    @Test
    fun focusRestartAndSkip() {
        assertEquals(FOCUS_WORK_MS, FocusState(remainingMs = 5).restarted().remainingMs)
        assertTrue(FocusState().next().onBreak)
    }

    @Test
    fun countdownFormatRoundsUpSoItStartsAtTheFullTime() {
        assertEquals("25:00", formatCountdown(FOCUS_WORK_MS))
        assertEquals("2:41", formatCountdown(160_500))
        assertEquals("0:00", formatCountdown(-5))
    }

    @Test
    fun trackWithTimeLeftUsesACountdown() {
        val face = NowPlayingStatus.Track("A", null, true, null, 90_000, 200_000).toFace()
        assertEquals(FaceValue.Countdown(90_000, true), face.value)
        val paused = NowPlayingStatus.Track("A", null, false, null, 90_000, 200_000).toFace()
        assertEquals(FaceValue.Countdown(90_000, false), paused.value)
    }

    @Test
    fun alertsCountStillInsideTheClover() {
        assertTrue(AlertsStatus.Count(1).toFace().valueInside)
    }
}
