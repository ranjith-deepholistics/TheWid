package com.fivehouse.thewid.widget

import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.BreatheLog
import com.fivehouse.thewid.data.personal.DayKind
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.FOCUS_SESSIONS
import com.fivehouse.thewid.data.personal.FocusState
import com.fivehouse.thewid.data.personal.MochiMood
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.WeekEvent
import com.fivehouse.thewid.ui.shapes.FaceExpression
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val NO_VALUE = "--"
private const val MAX_VALUE_CHARS = 10

/** A person's distinct colour, by their place in the circle. */
val PersonTints = listOf(
    Atmosphere.DuskBloom.gloss,
    GlossTint.Sun,
    Atmosphere.CoolMist.gloss,
    Atmosphere.Mint.gloss
)

private val DayKind.tint: GlossTint get() = if (this == DayKind.Deadline) GlossTint.Accent else Atmosphere.Stone.gloss

/** Monday to Sunday of the week holding [today]. Each day shows its first event's shape, or Circle if free. */
fun weekStrip(events: List<WeekEvent>, today: LocalDate): List<ShapeCatalog> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L until 7L).map { offset ->
        val day = monday.plusDays(offset)
        events.firstOrNull { it.date == day }?.kind?.shape ?: ShapeCatalog.Circle
    }
}

fun weekFace(events: List<WeekEvent>, today: LocalDate): StatusFace {
    val strip = weekStrip(events, today)
    val next = events.filter { !it.date.isBefore(today) }.minByOrNull { it.epochDay }
        ?: return StatusFace(
            ShapeCatalog.Circle,
            FaceValue.Literal(NO_VALUE),
            surface = ShapeSurface.Dashed,
            strip = strip
        )
    val days = ChronoUnit.DAYS.between(today, next.date).toInt()
    return StatusFace(
        shape = next.kind.shape,
        value = if (days == 0) {
            FaceValue.Resource(R.string.widget_value_today)
        } else {
            FaceValue.Plural(R.plurals.widget_value_days, days)
        },
        tint = next.kind.tint,
        isLive = days == 0,
        caption = next.label,
        strip = strip
    )
}

fun peopleFace(people: List<Person>): StatusFace {
    val first = people.firstOrNull()
        ?: return StatusFace(ShapeCatalog.Heart, FaceValue.Literal(NO_VALUE), surface = ShapeSurface.Dashed)
    return StatusFace(
        shape = first.shape,
        value = FaceValue.Literal(first.initial),
        tint = PersonTints.first(),
        caption = first.name,
        people = people
    )
}

fun Decision.toFace(): StatusFace {
    val chosen = result
    return when {
        !canRoll -> StatusFace(ShapeCatalog.Cookie6Sided, FaceValue.Literal(NO_VALUE), surface = ShapeSurface.Dashed)

        chosen == null -> StatusFace(
            ShapeCatalog.Cookie6Sided,
            FaceValue.Resource(R.string.widget_value_roll),
            tint = Atmosphere.Peach.gloss,
            caption = question.ifBlank { null },
            rollable = true
        )

        else -> StatusFace(
            chosen.shape,
            FaceValue.Literal(chosen.label.take(MAX_VALUE_CHARS)),
            tint = Atmosphere.Peach.gloss,
            caption = question.ifBlank { null },
            rollable = true
        )
    }
}

fun MochiMood.toFace(name: String): StatusFace = StatusFace(
    shape = when (this) {
        MochiMood.Excited -> ShapeCatalog.SoftBoom
        MochiMood.Sleepy -> ShapeCatalog.Ghostish
        MochiMood.Busy -> ShapeCatalog.SoftBurst
        else -> ShapeCatalog.Puffy
    },
    value = FaceValue.Resource(
        when (this) {
            MochiMood.Happy -> R.string.mochi_value_happy
            MochiMood.Excited -> R.string.mochi_value_excited
            MochiMood.Sleepy -> R.string.mochi_value_sleepy
            MochiMood.Dozing -> R.string.mochi_value_dozing
            MochiMood.Lonely -> R.string.mochi_value_lonely
            MochiMood.Busy -> R.string.mochi_value_busy
        }
    ),
    tint = if (this == MochiMood.Excited) GlossTint.Accent else Atmosphere.Stone.gloss,
    isLive = this == MochiMood.Excited,
    caption = name.ifBlank { null },
    expression = when (this) {
        MochiMood.Happy -> FaceExpression.Happy
        MochiMood.Excited -> FaceExpression.Excited
        MochiMood.Sleepy -> FaceExpression.Sleepy
        MochiMood.Dozing -> FaceExpression.Dozing
        MochiMood.Lonely -> FaceExpression.Lonely
        MochiMood.Busy -> FaceExpression.Busy
    }
)

fun BreatheLog.toFace(today: LocalDate): StatusFace = StatusFace(
    shape = ShapeCatalog.Flower,
    value = FaceValue.Plural(R.plurals.widget_value_sessions_today, sessionsOn(today)),
    tint = Atmosphere.Mint.gloss
)

fun FocusState.toFace(nowMs: Long): StatusFace = StatusFace(
    shape = if (onBreak) ShapeCatalog.Circle else ShapeCatalog.Sunny,
    value = FaceValue.Countdown(remainingAt(nowMs), isRunning),
    tint = Atmosphere.DuskBloom.gloss,
    isLive = isRunning,
    footnote = FaceValue.Format(
        if (onBreak) R.string.focus_break_of else R.string.focus_session_of,
        listOf(session, FOCUS_SESSIONS)
    )
)
