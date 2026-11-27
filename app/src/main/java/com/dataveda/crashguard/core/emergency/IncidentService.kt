package com.dataveda.crashguard.core.emergency

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.core.notifications.Notifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the process alive and visible while live location is being shared
 * during an active incident. The location loop itself lives in [EmergencyEscalationManager].
 */
@AndroidEntryPoint
class IncidentService : Service() {

    @Inject lateinit var notifier: Notifier

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this, Notifier.ID_SHARING, notifier.sharingNotification(),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            Log.w("IncidentService", "startForeground failed", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, IncidentService::class.java))

        fun stop(context: Context) = context.stopService(Intent(context, IncidentService::class.java))
    }
}
