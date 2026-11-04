package com.fivehouse.thewid.data.personal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import kotlin.coroutines.resume
import kotlin.math.round
import kotlinx.coroutines.suspendCancellableCoroutine

/** Rounded to about a kilometre, which is plenty for sunset and keeps less about where someone is. */
private fun Double.rounded() = round(this * 100) / 100

/** The phone's coarse position for working out sunset, or null without permission or a fix. Stays on the phone. */
suspend fun Context.coarsePosition(): Pair<Double, Double>? {
    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return null
    }
    val manager = getSystemService(LocationManager::class.java)
    val known = manager.getProviders(true).firstNotNullOfOrNull { provider ->
        try {
            manager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        }
    }
    val location: Location? = known ?: suspendCancellableCoroutine { continuation ->
        val provider = manager.getProviders(true).firstOrNull()
        if (provider == null) {
            continuation.resume(null)
        } else {
            manager.getCurrentLocation(provider, null, mainExecutor) { continuation.resume(it) }
        }
    }
    return location?.let { it.latitude.rounded() to it.longitude.rounded() }
}
