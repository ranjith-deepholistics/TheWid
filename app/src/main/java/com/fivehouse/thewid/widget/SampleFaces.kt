package com.fivehouse.thewid.widget

import com.fivehouse.thewid.data.personal.BreatheLog
import com.fivehouse.thewid.data.personal.DayKind
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.DecisionOption
import com.fivehouse.thewid.data.personal.FocusState
import com.fivehouse.thewid.data.personal.MochiMood
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.WeekEvent
import com.fivehouse.thewid.data.status.AlarmStatus
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.CellularNetwork
import com.fivehouse.thewid.data.status.CellularStatus
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.data.status.WifiStatus
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

private val SampleDay = LocalDate.of(2026, 11, 2)

/** Example content for previews and the launcher's widget picker. Never shown as real data. */
fun sampleFace(kind: StatusWidgetKind): StatusFace = when (kind) {
    StatusWidgetKind.Battery -> BatteryStatus(68, isCharging = true).toFace()

    StatusWidgetKind.Cellular -> CellularStatus.Signal(3, CellularNetwork.FiveG).toFace()

    StatusWidgetKind.Wifi -> WifiStatus.Connected(3).toFace()

    StatusWidgetKind.Sound -> SoundStatus.Silent.toFace()

    StatusWidgetKind.Alarm -> AlarmStatus.Scheduled(Instant.parse("2026-11-03T07:00:00Z")).toFace(ZoneOffset.UTC, true)

    StatusWidgetKind.Time -> TimeStatus(ZonedDateTime.parse("2026-11-02T09:42:00+05:30"), isDay = true).toFace()

    StatusWidgetKind.Alerts -> AlertsStatus.Count(2).toFace()

    StatusWidgetKind.NowPlaying -> NowPlayingStatus.Track("Track name", "Artist", true, null).toFace()

    StatusWidgetKind.Week -> weekFace(
        listOf(WeekEvent("sample", SampleDay.plusDays(3).toEpochDay(), "Your event", DayKind.Deadline)),
        SampleDay
    )

    StatusWidgetKind.InnerCircle -> peopleFace(listOf(Person("", "Partner", ShapeCatalog.Heart)))

    StatusWidgetKind.Decide -> Decision(
        "Where to eat?",
        listOf(DecisionOption("Dosa", ShapeCatalog.Cookie6Sided), DecisionOption("Pizza", ShapeCatalog.Diamond)),
        lastIndex = 0
    ).toFace()

    StatusWidgetKind.Mochi -> MochiMood.Happy.toFace("Mochi")

    StatusWidgetKind.Breathe -> BreatheLog(SampleDay.toEpochDay(), 2).toFace(SampleDay)

    StatusWidgetKind.Focus -> FocusState().toFace(0)
}
