package com.fivehouse.thewid.data.personal

import com.fivehouse.thewid.ui.shapes.FaceExpression
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.widget.FaceValue
import com.fivehouse.thewid.widget.peopleFace
import com.fivehouse.thewid.widget.toFace
import com.fivehouse.thewid.widget.weekFace
import com.fivehouse.thewid.widget.weekStrip
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalDataTest {
    private val monday = LocalDate.of(2026, 11, 2)

    private fun event(day: LocalDate, kind: DayKind = DayKind.Deadline, label: String = "Thing") =
        WeekEvent(epochDay = day.toEpochDay(), label = label, kind = kind)

    @Test
    fun weekCountsDownToTheNextEventAndIgnoresPastOnes() {
        val face = weekFace(listOf(event(monday.minusDays(1)), event(monday.plusDays(3), DayKind.Trip)), monday)
        assertEquals(ShapeCatalog.Arrow, face.shape)
        assertEquals(3, (face.value as FaceValue.Plural).count)
        assertEquals("Thing", face.caption)
    }

    @Test
    fun weekSaysTodayAndIsDashedWithNothingAhead() {
        assertTrue(weekFace(listOf(event(monday)), monday).value is FaceValue.Resource)
        assertEquals(ShapeSurface.Dashed, weekFace(emptyList(), monday).surface)
    }

    @Test
    fun weekStripRunsMondayToSundayWithCircleForFreeDays() {
        val strip = weekStrip(listOf(event(monday.plusDays(2), DayKind.Birthday)), monday.plusDays(4))
        assertEquals(7, strip.size)
        assertEquals(ShapeCatalog.Heart, strip[2])
        assertEquals(ShapeCatalog.Circle, strip[0])
    }

    @Test
    fun specShapesForEachKindOfDay() {
        assertEquals(ShapeCatalog.Diamond, DayKind.Deadline.shape)
        assertEquals(ShapeCatalog.Burst, DayKind.Busy.shape)
        assertEquals(ShapeCatalog.Heart, DayKind.Birthday.shape)
        assertEquals(ShapeCatalog.Arrow, DayKind.Trip.shape)
    }

    @Test
    fun personInitialIsUppercaseFirstLetter() {
        assertEquals("S", Person("uri", "  sam", ShapeCatalog.Flower).initial)
    }

    @Test
    fun peopleFaceIsDashedWhenEmptyAndCarriesEveryoneOtherwise() {
        assertEquals(ShapeSurface.Dashed, peopleFace(emptyList()).surface)
        val people = listOf(Person("a", "Ann", ShapeCatalog.Heart), Person("b", "Bo", ShapeCatalog.Flower))
        assertEquals(people, peopleFace(people).people)
    }

    private val twoOptions = Decision(
        "Eat?",
        listOf(DecisionOption("Dosa", ShapeCatalog.Cookie6Sided), DecisionOption("Pizza", ShapeCatalog.Diamond))
    )

    @Test
    fun aRollNeverRepeatsTheLastResult() {
        var current = twoOptions.rolled(Random(1))
        repeat(20) {
            val next = current.rolled(Random(it))
            assertNotEquals(current.lastIndex, next.lastIndex)
            current = next
        }
    }

    @Test
    fun tooFewOptionsCannotRoll() {
        val one = Decision("Eat?", listOf(DecisionOption("Dosa", ShapeCatalog.Cookie6Sided)))
        assertFalse(one.canRoll)
        assertNull(one.rolled().lastIndex)
        assertEquals(ShapeSurface.Dashed, one.toFace().surface)
    }

    @Test
    fun rolledFaceShowsTheChosenOptionsShape() {
        val face = twoOptions.copy(lastIndex = 1).toFace()
        assertEquals(ShapeCatalog.Diamond, face.shape)
        assertEquals(FaceValue.Literal("Pizza"), face.value)
        assertTrue(face.rollable)
    }

    private fun mood(
        settings: MochiSettings = MochiSettings(),
        charging: Boolean = false,
        battery: Int? = 60,
        silent: Boolean = false,
        wifiOff: Boolean = false,
        alerts: Int? = null
    ) = moodFor(settings, charging, battery, silent, wifiOff, alerts)

    @Test
    fun mochiMoodsFollowThePhone() {
        assertEquals(MochiMood.Happy, mood())
        assertEquals(MochiMood.Excited, mood(charging = true, battery = 10))
        assertEquals(MochiMood.Sleepy, mood(battery = 14))
        assertEquals(MochiMood.Happy, mood(battery = 15))
        assertEquals(MochiMood.Dozing, mood(silent = true))
        assertEquals(MochiMood.Lonely, mood(wifiOff = true))
    }

    @Test
    fun switchedOffMoodsAreIgnored() {
        assertEquals(MochiMood.Happy, mood(MochiSettings(charging = false), charging = true))
        assertEquals(MochiMood.Happy, mood(alerts = 9)) // Busy is off by default.
        assertEquals(MochiMood.Busy, mood(MochiSettings(busy = true), alerts = 5))
        assertEquals(MochiMood.Happy, mood(MochiSettings(busy = true), alerts = null))
    }

    @Test
    fun mochiUsesTheSpecShapes() {
        assertEquals(ShapeCatalog.Puffy, MochiMood.Happy.toFace("M").shape)
        assertEquals(ShapeCatalog.Ghostish, MochiMood.Sleepy.toFace("M").shape)
        assertEquals(ShapeCatalog.SoftBoom, MochiMood.Excited.toFace("M").shape)
        assertEquals(FaceExpression.Excited, MochiMood.Excited.toFace("M").expression)
    }

    @Test
    fun breatheLogCountsPerDayAndResetsTomorrow() {
        val log = BreatheLog().afterSession(monday).afterSession(monday)
        assertEquals(2, log.sessionsOn(monday))
        assertEquals(0, log.sessionsOn(monday.plusDays(1)))
        assertEquals(1, log.afterSession(monday.plusDays(1)).sessionsOn(monday.plusDays(1)))
    }

    @Test
    fun breathPhasesAndRoundsFollowTheClock() {
        assertEquals(BreathState(1, BreathPhase.Inhale, 4, 0f, false), breathStateAt(0))
        assertEquals(BreathPhase.Hold, breathStateAt(4_000).phase)
        assertEquals(7, breathStateAt(4_000).secondsLeft)
        assertEquals(BreathPhase.Exhale, breathStateAt(11_000).phase)
        assertEquals(2, breathStateAt(19_000).round)
    }

    @Test
    fun aSessionCompletesAfterItsLastRound() {
        assertTrue(breathStateAt(SessionMillis).isComplete)
        assertFalse(breathStateAt(SessionMillis - 1).isComplete)
        assertEquals(BREATHE_ROUNDS, breathStateAt(SessionMillis - 1).round)
    }
}
