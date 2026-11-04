package com.fivehouse.thewid.data.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

private const val ZENITH_DEGREES = 90.833

data class SunTimes(val sunrise: ZonedDateTime, val sunset: ZonedDateTime)

/**
 * Sunrise and sunset calculated on the device, with no network, using the standard almanac method.
 * Null when the sun doesn't rise or set that day, as in polar summer and winter.
 */
fun sunTimes(date: LocalDate, latitude: Double, longitude: Double, zone: ZoneId): SunTimes? {
    val rise = sunEventUtcHours(date, latitude, longitude, rising = true) ?: return null
    val set = sunEventUtcHours(date, latitude, longitude, rising = false) ?: return null
    return SunTimes(atUtcHours(date, rise, zone), atUtcHours(date, set, zone))
}

/** The event's UTC time of day, placed on the UTC date that makes it fall on [date] locally. */
private fun atUtcHours(date: LocalDate, hours: Double, zone: ZoneId): ZonedDateTime {
    val wrapped = hours - floor(hours / 24) * 24
    val secondOfDay = (wrapped * 3600).toLong().coerceIn(0, LocalTime.MAX.toSecondOfDay().toLong())
    val first = date.atTime(LocalTime.ofSecondOfDay(secondOfDay)).atZone(ZoneOffset.UTC).withZoneSameInstant(zone)
    return first.plusDays(ChronoUnit.DAYS.between(first.toLocalDate(), date))
}

private fun sunEventUtcHours(date: LocalDate, latitude: Double, longitude: Double, rising: Boolean): Double? {
    val longitudeHours = longitude / 15
    val approximate = date.dayOfYear + ((if (rising) 6 else 18) - longitudeHours) / 24
    val anomaly = 0.9856 * approximate - 3.289
    val trueLongitude = normalizeDegrees(
        anomaly + 1.916 * sinDegrees(anomaly) + 0.020 * sinDegrees(2 * anomaly) + 282.634
    )
    var ascension = normalizeDegrees(degrees(atan(0.91764 * tanDegrees(trueLongitude))))
    ascension += floor(trueLongitude / 90) * 90 - floor(ascension / 90) * 90
    val ascensionHours = ascension / 15
    val sinDeclination = 0.39782 * sinDegrees(trueLongitude)
    val cosDeclination = cos(asin(sinDeclination))
    val cosHour = (cosDegrees(ZENITH_DEGREES) - sinDeclination * sinDegrees(latitude)) /
        (cosDeclination * cosDegrees(latitude))
    if (cosHour > 1 || cosHour < -1) return null
    val hourAngle = degrees(acos(cosHour)).let { if (rising) 360 - it else it } / 15
    val local = hourAngle + ascensionHours - 0.06571 * approximate - 6.622
    return local - longitudeHours
}

private fun normalizeDegrees(value: Double) = degrees(atan2(sinDegrees(value), cosDegrees(value))).let {
    if (it < 0) it + 360 else it
}

private fun sinDegrees(value: Double) = sin(Math.toRadians(value))

private fun cosDegrees(value: Double) = cos(Math.toRadians(value))

private fun tanDegrees(value: Double) = tan(Math.toRadians(value))

private fun degrees(radians: Double) = Math.toDegrees(radians)
