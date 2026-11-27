package com.dataveda.crashguard.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.domain.models.GeoFix
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(@ApplicationContext private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Current fix with timeout; falls back to last known location (flagged) if GPS is unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun capture(timeoutMs: Long = 8_000): GeoFix? {
        if (!hasPermission()) return null
        val cts = CancellationTokenSource()
        val current = runCatching {
            withTimeoutOrNull(timeoutMs) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }
        }.getOrNull()
        cts.cancel()
        if (current != null) return current.toFix(isLastKnown = false)
        return lastKnown()
    }

    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): GeoFix? {
        if (!hasPermission()) return null
        return runCatching { client.lastLocation.await() }.getOrNull()?.toFix(isLastKnown = true)
    }

    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long): Flow<Location> {
        if (!hasPermission()) return emptyFlow()
        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { trySend(it) }
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            awaitClose { client.removeLocationUpdates(callback) }
        }
    }

    private fun Location.toFix(isLastKnown: Boolean) =
        GeoFix(latitude, longitude, if (hasAccuracy()) accuracy else null, time, isLastKnown)
}
