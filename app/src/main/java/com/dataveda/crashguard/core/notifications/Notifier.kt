package com.dataveda.crashguard.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.MainActivity
import com.dataveda.crashguard.R
import com.dataveda.crashguard.ui.crash.CrashAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CH_TRIP, context.getString(R.string.channel_trip), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_CRASH, context.getString(R.string.channel_crash), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null) // alarm is played by AlarmPlayer with USAGE_ALARM
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(CH_SOS, context.getString(R.string.channel_sos), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    private fun canNotify() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: String, requestCode: Int, extras: Intent.() -> Unit = {}): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, NotificationActionReceiver::class.java).setAction(action).apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun crashAlertActivityIntent(): PendingIntent = PendingIntent.getActivity(
        context, 2,
        Intent(context, CrashAlertActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun tripNotification(startedAt: Long, confirming: Boolean, simulated: Boolean): Notification {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(startedAt))
        val title = when {
            confirming -> context.getString(R.string.notif_trip_confirming)
            simulated -> context.getString(R.string.notif_trip_active_sim)
            else -> context.getString(R.string.notif_trip_active)
        }
        return NotificationCompat.Builder(context, CH_TRIP)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_trip_started_at, time))
            .setSubText(context.getString(R.string.notif_tap_to_open))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.action_stop_trip), action(NotificationActionReceiver.ACTION_STOP_TRIP, 10))
            .build()
    }

    fun showRolePrompt() {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(context.getString(R.string.role_question))
            .setContentText(context.getString(R.string.role_question_body))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.role_driver), action(NotificationActionReceiver.ACTION_ROLE_DRIVER, 20))
            .addAction(0, context.getString(R.string.role_passenger), action(NotificationActionReceiver.ACTION_ROLE_PASSENGER, 21))
            .addAction(0, context.getString(R.string.role_not_driving), action(NotificationActionReceiver.ACTION_ROLE_NOT_DRIVING, 22))
            .build()
        nm.notify(ID_ROLE, n)
    }

    fun cancelRolePrompt() = nm.cancel(ID_ROLE)

    fun showCrashAlert(secondsLeft: Int) {
        if (!canNotify()) return
        val full = crashAlertActivityIntent()
        val n = NotificationCompat.Builder(context, CH_CRASH)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(context.getString(R.string.crash_title))
            .setContentText(context.getString(R.string.crash_countdown_notif, secondsLeft))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(context, R.color.cg_emergency))
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(0, context.getString(R.string.crash_im_okay), action(NotificationActionReceiver.ACTION_OKAY, 30))
            .addAction(0, context.getString(R.string.crash_need_help), action(NotificationActionReceiver.ACTION_NEED_HELP, 31))
            .build()
        nm.notify(ID_CRASH, n)
    }

    fun showAreYouOkay(incidentId: String) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, CH_SOS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(context.getString(R.string.soft_check_title))
            .setContentText(context.getString(R.string.soft_check_body))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.crash_im_okay),
                action(NotificationActionReceiver.ACTION_SOFT_OKAY, 40) { putExtra(NotificationActionReceiver.EXTRA_INCIDENT, incidentId) })
            .addAction(0, context.getString(R.string.crash_need_help),
                action(NotificationActionReceiver.ACTION_SOFT_HELP, 41) { putExtra(NotificationActionReceiver.EXTRA_INCIDENT, incidentId) })
            .build()
        nm.notify(ID_SOFT, n)
    }

    fun cancelSoftCheck() = nm.cancel(ID_SOFT)

    fun cancelCrashAlert() = nm.cancel(ID_CRASH)

    fun sharingNotification(): Notification =
        NotificationCompat.Builder(context, CH_SOS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(context.getString(R.string.notif_sharing_title))
            .setContentText(context.getString(R.string.notif_sharing_body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(context, R.color.cg_emergency))
            .setContentIntent(crashAlertActivityIntent())
            .addAction(0, context.getString(R.string.sos_stop_sharing), action(NotificationActionReceiver.ACTION_STOP_SHARING, 50))
            .build()

    fun showWarning(title: String, body: String) {
        if (!canNotify()) return
        nm.notify(
            ID_WARNING,
            NotificationCompat.Builder(context, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(openAppIntent())
                .build()
        )
    }

    companion object {
        const val CH_TRIP = "trip"
        const val CH_CRASH = "crash_alert"
        const val CH_SOS = "sos"
        const val CH_STATUS = "status"

        const val ID_TRIP = 1001
        const val ID_CRASH = 1002
        const val ID_SOFT = 1003
        const val ID_SHARING = 1004
        const val ID_ROLE = 1005
        const val ID_WARNING = 1006
    }
}
