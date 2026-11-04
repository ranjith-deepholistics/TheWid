package com.fivehouse.thewid.data.personal

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

private const val CALENDAR_PAST_DAYS = 7L
private const val CALENDAR_FUTURE_DAYS = 60L

/** Read-only calendar events as Busy days. Empty without permission, which is only asked for when tapped. */
fun Context.calendarEvents(): Flow<List<WeekEvent>> {
    if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return flowOf(emptyList())
    }
    return callbackFlow {
        val publish = { trySend(queryCalendar()) }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                publish()
            }
        }
        contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observer)
        publish()
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }
}

private fun Context.queryCalendar(): List<WeekEvent> {
    val now = System.currentTimeMillis()
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, now - TimeUnit.DAYS.toMillis(CALENDAR_PAST_DAYS))
        ContentUris.appendId(it, now + TimeUnit.DAYS.toMillis(CALENDAR_FUTURE_DAYS))
    }.build()
    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY
    )
    return try {
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    // All-day events are stored at UTC midnight, others in the phone's own time.
                    val zone = if (cursor.getInt(3) == 1) ZoneOffset.UTC else ZoneId.systemDefault()
                    val date = Instant.ofEpochMilli(cursor.getLong(1)).atZone(zone).toLocalDate()
                    add(
                        WeekEvent(
                            "calendar-${cursor.getLong(0)}-${date.toEpochDay()}",
                            date.toEpochDay(),
                            cursor.getString(2).orEmpty(),
                            DayKind.Busy
                        )
                    )
                }
            }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }
}
