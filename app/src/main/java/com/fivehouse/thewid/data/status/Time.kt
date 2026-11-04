package com.fivehouse.thewid.data.status

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// The spec picks a day and a night shape but doesn't define the hours.
private const val DAY_START_HOUR = 6
private const val NIGHT_START_HOUR = 18

/** @property dateTime the current local time, to the minute. */
data class TimeStatus(val dateTime: ZonedDateTime, val isDay: Boolean)

interface TimeSource {
    /** The current time, emitted on start and whenever the clock or time zone moves. */
    val now: Flow<ZonedDateTime>
}

class TimeRepository(source: TimeSource) {
    val status: Flow<TimeStatus> = source.now
        .map { now ->
            TimeStatus(
                dateTime = now.truncatedTo(ChronoUnit.MINUTES),
                isDay = now.hour in DAY_START_HOUR until NIGHT_START_HOUR
            )
        }
        .distinctUntilChanged()
}
