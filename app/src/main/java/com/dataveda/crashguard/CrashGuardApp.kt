package com.dataveda.crashguard

import android.app.Application
import com.dataveda.crashguard.core.notifications.Notifier
import com.dataveda.crashguard.core.trip.TripDetectionManager
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.repository.IncidentRepository
import com.dataveda.crashguard.data.repository.TripRepository
import com.dataveda.crashguard.di.AppScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CrashGuardApp : Application() {

    @Inject lateinit var notifier: Notifier
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var tripManager: TripDetectionManager
    @Inject lateinit var trips: TripRepository
    @Inject lateinit var incidents: IncidentRepository
    @Inject @AppScope lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        val processStart = System.currentTimeMillis()
        scope.launch {
            // Fresh process: nothing can be actively monitored, so close anything left open by a kill.
            trips.closeDangling(processStart)
            incidents.resolveDangling(processStart)
            if (settings.current().protectionEnabled) tripManager.registerTransitions()
        }
    }
}
