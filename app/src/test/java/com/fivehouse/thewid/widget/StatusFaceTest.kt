package com.fivehouse.thewid.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.status.AlarmStatus
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.CellularNetwork
import com.fivehouse.thewid.data.status.CellularStatus
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.data.status.WifiStatus
import com.fivehouse.thewid.ui.shapes.ClockMarks
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.GlossTint
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusFaceTest {
    @Test
    fun batteryFillsToItsLevelAndTurnsAccentWhileCharging() {
        val resting = BatteryStatus(68, isCharging = false).toFace()
        assertEquals(ShapeCatalog.SoftBurst, resting.shape)
        assertEquals(0.68f, resting.fill, 0.001f)
        assertEquals(FaceValue.Literal("68%"), resting.value)
        assertFalse(resting.isLive)

        val charging = BatteryStatus(68, isCharging = true).toFace()
        assertEquals(GlossTint.Accent, charging.tint)
        assertTrue(charging.isLive)
    }

    @Test
    fun glowOffKeepsTheNormalTintButStillShowsCharging() {
        val face = BatteryStatus(68, isCharging = true).toFace(glow = false)
        assertFalse(face.tint == GlossTint.Accent)
        assertTrue(face.isLive)
    }

    @Test
    fun unknownBatteryIsDashed() {
        assertEquals(ShapeSurface.Dashed, BatteryStatus(null, isCharging = false).toFace().surface)
    }

    @Test
    fun chosenShapeReplacesTheDefault() {
        assertEquals(ShapeCatalog.Circle, BatteryStatus(50, false).toFace(ShapeCatalog.Circle).shape)
    }

    @Test
    fun cellularFillsByBarsAndLabelsTheNetwork() {
        val face = CellularStatus.Signal(3, CellularNetwork.FiveG).toFace()
        assertEquals(ShapeCatalog.Pentagon, face.shape)
        assertEquals(0.75f, face.fill, 0.001f)
        assertEquals(FaceValue.Literal("5G"), face.value)
    }

    @Test
    fun cellularWithoutMobileDataShowsBarsInstead() {
        assertEquals(FaceValue.Literal("2/4"), CellularStatus.Signal(2, null).toFace().value)
    }

    @Test
    fun unavailableCellularIsDashed() {
        assertEquals(ShapeSurface.Dashed, CellularStatus.Unavailable.toFace().surface)
    }

    @Test
    fun wifiIsADashedFanWhenOff() {
        val face = WifiStatus.Off.toFace()
        assertEquals(ShapeCatalog.Fan, face.shape)
        assertEquals(ShapeSurface.Dashed, face.surface)
        assertEquals(FaceValue.Resource(R.string.widget_value_off), face.value)
    }

    @Test
    fun wifiFillsByBarsAndUnknownStrengthIsFull() {
        assertEquals(0.5f, WifiStatus.Connected(2).toFace().fill, 0.001f)
        assertEquals(1f, WifiStatus.Connected(null).toFace().fill, 0.001f)
    }

    @Test
    fun soundShapeReportsTheMode() {
        assertEquals(ShapeCatalog.Circle, SoundStatus.Ring.toFace().shape)
        assertEquals(ShapeCatalog.Cookie12Sided, SoundStatus.Vibrate.toFace().shape)
        assertEquals(ShapeCatalog.Ghostish, SoundStatus.Silent.toFace().shape)
    }

    @Test
    fun alarmShowsLocalTimeInTheChosenFormat() {
        val at = Instant.parse("2026-11-03T01:30:00Z")
        val zone = ZoneId.of("Asia/Kolkata")
        assertEquals(FaceValue.Literal("07:00"), AlarmStatus.Scheduled(at).toFace(zone, is24Hour = true).value)
        assertEquals(FaceValue.Literal("7:00"), AlarmStatus.Scheduled(at).toFace(zone, is24Hour = false).value)
        assertEquals(ShapeCatalog.SemiCircle, AlarmStatus.Scheduled(at).toFace(zone, true).shape)
    }

    @Test
    fun noAlarmIsDashed() {
        assertEquals(ShapeSurface.Dashed, AlarmStatus.None.toFace(ZoneId.of("UTC"), true).surface)
    }

    @Test
    fun timeIsGoldSunByDayAndCookieByNight() {
        val day = TimeStatus(ZonedDateTime.parse("2026-11-02T09:42:00+05:30"), isDay = true).toFace()
        assertEquals(ShapeCatalog.Sunny, day.shape)
        assertEquals(GlossTint.Sun, day.tint)
        assertEquals(FaceValue.LiveClock, day.value)

        val night = TimeStatus(ZonedDateTime.parse("2026-11-02T21:00:00+05:30"), isDay = false).toFace()
        assertEquals(ShapeCatalog.Cookie12Sided, night.shape)
    }

    @Test
    fun clockMarksFollowTheTime() {
        val at = TimeStatus(ZonedDateTime.parse("2026-11-02T15:30:00+05:30"), isDay = true).toFace()
        assertEquals(ClockMarks(hourDegrees = 105f, minuteDegrees = 180f), at.clock)
    }

    @Test
    fun layoutFollowsTheWidgetsRealSize() {
        assertEquals(WidgetLayout.ShapeOnly, widgetLayoutFor(DpSize(80.dp, 90.dp)))
        assertEquals(WidgetLayout.ShapeOnly, widgetLayoutFor(DpSize(340.dp, 90.dp)))
        assertEquals(WidgetLayout.Square, widgetLayoutFor(DpSize(170.dp, 180.dp)))
        assertEquals(WidgetLayout.Wide, widgetLayoutFor(DpSize(340.dp, 180.dp)))
        assertEquals(WidgetLayout.Wide, widgetLayoutFor(DpSize(340.dp, 370.dp)))
    }

    @Test
    fun everyKindOffersTheSpecShapeFirstOrNoPicker() {
        assertEquals(ShapeCatalog.SoftBurst, StatusWidgetKind.Battery.shapeOptions.first())
        assertEquals(ShapeCatalog.Pentagon, StatusWidgetKind.Cellular.shapeOptions.first())
        assertEquals(ShapeCatalog.Fan, StatusWidgetKind.Wifi.shapeOptions.first())
        assertEquals(ShapeCatalog.SemiCircle, StatusWidgetKind.Alarm.shapeOptions.first())
        assertTrue(StatusWidgetKind.Sound.shapeOptions.isEmpty())
        assertTrue(StatusWidgetKind.Time.shapeOptions.isEmpty())
    }
}
