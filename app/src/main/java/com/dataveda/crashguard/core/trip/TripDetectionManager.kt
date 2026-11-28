package com.dataveda.crashguard.core.trip

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.domain.models.TripRole
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class TripMode { AUTO, MANUAL, SIMULATED }

sealed interface TripState {
    data object Idle : TripState
    data class Confirming(val startedAt: Long, val speedKmh: Double = 0.0) : TripState
    data class Active(
        val tripId: String,
        val startedAt: Long,
        val mode: TripMode,
        val speedKmh: Double = 0.0,
        val distanceKm: Double = 0.0,
        val maxSpeedKmh: Double = 0.0,
        val role: TripRole = TripRole.UNKNOWN,
        val bluetoothCar: Boolean = false,
    ) : TripState
}

/**
 * Subscribes to Activity Recognition IN_VEHICLE transitions and owns the observable trip lifecycle.
 * The heavy lifting (GPS confirmation, sensors) happens in [TripMonitoringService].
 */
@Singleton
class TripDetectionManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val _state = MutableStateFlow<TripState>(TripState.Idle)
    val state: StateFlow<TripState> = _state.asStateFlow()

    @Volatile
    private var interactionListener: ((Long) -> Unit)? = null

    internal fun set(state: TripState) { _state.value = state }
    internal fun update(block: (TripState) -> TripState) = _state.update(block)
    internal fun setInteractionListener(listener: ((Long) -> Unit)?) { interactionListener = listener }

    /** Called by MainActivity on touch events so post-impact screen interaction is fed into CrashDetectionEngine. */
    fun notifyUserInteraction(tMs: Long = System.currentTimeMillis()) {
        interactionListener?.invoke(tMs)
    }

    fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun transitionIntent(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(
            context, 100,
            Intent(context, ActivityTransitionReceiver::class.java).setAction(ActivityTransitionReceiver.ACTION_TRANSITION),
            flags,
        )
    }

    @SuppressLint("MissingPermission")
    fun registerTransitions(): Boolean {
        if (!hasActivityPermission()) return false
        val transitions = listOf(
            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
        )
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), transitionIntent())
            .addOnSuccessListener { Log.i(TAG, "Activity transitions registered") }
            .addOnFailureListener { Log.w(TAG, "Activity transitions failed", it) }
        return true
    }

    @SuppressLint("MissingPermission")
    fun unregisterTransitions() {
        if (!hasActivityPermission()) return
        ActivityRecognition.getClient(context).removeActivityTransitionUpdates(transitionIntent())
    }

    fun startTrip(mode: TripMode) = TripMonitoringService.start(context, mode)

    fun stopTrip() = TripMonitoringService.stop(context)

    fun setRole(role: TripRole) {
        if (role == TripRole.NOT_DRIVING) {
            stopTrip()
        } else {
            _state.update { if (it is TripState.Active) it.copy(role = role) else it }
        }
    }

    val isAutoTrip: Boolean
        get() = when (val s = _state.value) {
            is TripState.Confirming -> true
            is TripState.Active -> s.mode == TripMode.AUTO
            TripState.Idle -> false
        }

    private companion object { const val TAG = "TripDetection" }
}
