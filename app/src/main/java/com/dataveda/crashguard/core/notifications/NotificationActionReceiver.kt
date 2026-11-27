package com.dataveda.crashguard.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dataveda.crashguard.core.emergency.EmergencyEscalationManager
import com.dataveda.crashguard.core.trip.TripDetectionManager
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.domain.models.UserResponse
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var escalation: EmergencyEscalationManager
    @Inject lateinit var tripManager: TripDetectionManager
    @Inject lateinit var notifier: Notifier

    override fun onReceive(context: Context, intent: Intent) {
        val incidentId = intent.getStringExtra(EXTRA_INCIDENT)
        when (intent.action) {
            ACTION_OKAY -> escalation.respond(UserResponse.OKAY)
            ACTION_NEED_HELP -> escalation.respond(UserResponse.NEED_HELP)
            ACTION_FALSE_ALARM -> escalation.respond(UserResponse.FALSE_ALARM)
            ACTION_SOFT_OKAY -> incidentId?.let { escalation.softResponse(it, UserResponse.OKAY) }
            ACTION_SOFT_HELP -> incidentId?.let { escalation.softResponse(it, UserResponse.NEED_HELP) }
            ACTION_STOP_TRIP -> tripManager.stopTrip()
            ACTION_STOP_SHARING -> escalation.stopSharing()
            ACTION_ROLE_DRIVER -> setRole(TripRole.DRIVER)
            ACTION_ROLE_PASSENGER -> setRole(TripRole.PASSENGER)
            ACTION_ROLE_NOT_DRIVING -> setRole(TripRole.NOT_DRIVING)
        }
    }

    private fun setRole(role: TripRole) {
        tripManager.setRole(role)
        notifier.cancelRolePrompt()
    }

    companion object {
        const val EXTRA_INCIDENT = "incident_id"
        const val ACTION_OKAY = "com.dataveda.crashguard.OKAY"
        const val ACTION_NEED_HELP = "com.dataveda.crashguard.NEED_HELP"
        const val ACTION_FALSE_ALARM = "com.dataveda.crashguard.FALSE_ALARM"
        const val ACTION_SOFT_OKAY = "com.dataveda.crashguard.SOFT_OKAY"
        const val ACTION_SOFT_HELP = "com.dataveda.crashguard.SOFT_HELP"
        const val ACTION_STOP_TRIP = "com.dataveda.crashguard.STOP_TRIP"
        const val ACTION_STOP_SHARING = "com.dataveda.crashguard.STOP_SHARING"
        const val ACTION_ROLE_DRIVER = "com.dataveda.crashguard.ROLE_DRIVER"
        const val ACTION_ROLE_PASSENGER = "com.dataveda.crashguard.ROLE_PASSENGER"
        const val ACTION_ROLE_NOT_DRIVING = "com.dataveda.crashguard.ROLE_NOT_DRIVING"
    }
}
