package com.dataveda.crashguard.core.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.repository.IncidentRepository
import com.dataveda.crashguard.data.repository.TripRepository
import com.dataveda.crashguard.di.AppScope
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {
    @Inject lateinit var tripManager: TripDetectionManager
    @Inject lateinit var settings: SettingsRepository
    @Inject @AppScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                if (!settings.current().protectionEnabled) return@launch
                result.transitionEvents.filter { it.activityType == DetectedActivity.IN_VEHICLE }.forEach { e ->
                    when (e.transitionType) {
                        ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                            Log.i(TAG, "IN_VEHICLE enter")
                            runCatching { tripManager.startTrip(TripMode.AUTO) }
                                .onFailure { Log.w(TAG, "Could not start trip service", it) }
                        }
                        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                            Log.i(TAG, "IN_VEHICLE exit")
                            if (tripManager.isAutoTrip) tripManager.stopTrip()
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRANSITION = "com.dataveda.crashguard.ACTIVITY_TRANSITION"
        private const val TAG = "TransitionReceiver"
    }
}

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var tripManager: TripDetectionManager
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var trips: TripRepository
    @Inject lateinit var incidents: IncidentRepository
    @Inject @AppScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                trips.closeDangling()
                incidents.resolveDangling()
                if (settings.current().protectionEnabled) tripManager.registerTransitions()
            } finally {
                pending.finish()
            }
        }
    }
}
