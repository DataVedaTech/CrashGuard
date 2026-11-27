package com.dataveda.crashguard.core.emergency

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.data.local.ContactEntity
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.repository.IncidentRepository
import com.dataveda.crashguard.di.AppScope
import com.dataveda.crashguard.domain.models.AlertKind
import com.dataveda.crashguard.domain.models.DeliveryStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_SMS_ATTEMPTS = 3

/**
 * Sends alerts to emergency contacts. Every delivery is persisted first (offline-safe), then sent by SMS
 * if the user opted in and granted SEND_SMS. Otherwise it is flagged MANUAL_REQUIRED so the UI can offer
 * "Send via SMS app" / "Share" — the user is never left with a silent failure.
 */
@Singleton
class AlertDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidents: IncidentRepository,
    private val settings: SettingsRepository,
) {
    fun canSendSms(smsEnabled: Boolean): Boolean =
        smsEnabled &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    suspend fun dispatch(incidentId: String, contacts: List<ContactEntity>, kind: AlertKind, message: String) {
        val sms = canSendSms(settings.current().smsEnabled)
        contacts.forEach { c ->
            val id = incidents.addDelivery(
                AlertDeliveryEntity(
                    incidentId = incidentId, contactId = c.id, contactName = c.name, phone = c.phone,
                    kind = kind, message = message,
                    status = if (sms) DeliveryStatus.QUEUED else DeliveryStatus.MANUAL_REQUIRED,
                )
            )
            if (sms) sendSms(id)
        }
    }

    suspend fun sendSms(deliveryId: Long) {
        val d = incidents.delivery(deliveryId) ?: return
        incidents.markAttempt(deliveryId)
        try {
            val sm: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION") SmsManager.getDefault()
            }
            val parts = sm.divideMessage(d.message)
            val sent = ArrayList<PendingIntent>()
            val delivered = ArrayList<PendingIntent>()
            parts.indices.forEach { i ->
                sent += statusIntent(SmsStatusReceiver.ACTION_SENT, deliveryId, i, parts.size)
                delivered += statusIntent(SmsStatusReceiver.ACTION_DELIVERED, deliveryId, i, parts.size)
            }
            sm.sendMultipartTextMessage(d.phone, null, parts, sent, delivered)
        } catch (e: Exception) {
            Log.w(TAG, "SMS send failed for delivery $deliveryId", e)
            incidents.updateDelivery(deliveryId, DeliveryStatus.FAILED, e.javaClass.simpleName)
            scheduleRetry(context)
        }
    }

    private fun statusIntent(action: String, deliveryId: Long, part: Int, total: Int): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("crashguard://sms/$action/$deliveryId/$part") // unique per part
            putExtra(SmsStatusReceiver.EXTRA_DELIVERY_ID, deliveryId)
            putExtra(SmsStatusReceiver.EXTRA_LAST_PART, part == total - 1)
        }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Opens the default SMS app pre-filled for all contacts (manual fallback, no permission needed). */
    fun composeIntent(phones: List<String>, message: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phones.joinToString(if (Build.MANUFACTURER.equals("samsung", true)) "," else ";")))
            .putExtra("sms_body", message)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun shareIntent(message: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message), null
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    companion object {
        private const val TAG = "AlertDispatcher"

        fun scheduleRetry(context: Context) {
            val req = OneTimeWorkRequestBuilder<AlertRetryWorker>()
                .setInitialDelay(15, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("alert-retry", ExistingWorkPolicy.REPLACE, req)
        }
    }
}

@AndroidEntryPoint
class SmsStatusReceiver : BroadcastReceiver() {
    @Inject lateinit var incidents: IncidentRepository
    @Inject @AppScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_DELIVERY_ID, -1)
        if (id < 0) return
        val code = resultCode
        val ok = code == Activity.RESULT_OK
        val lastPart = intent.getBooleanExtra(EXTRA_LAST_PART, true)
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SENT -> if (!ok) {
                        incidents.updateDelivery(id, DeliveryStatus.FAILED, "SMS error code $code")
                        AlertDispatcher.scheduleRetry(context)
                    } else if (lastPart) {
                        val cur = incidents.delivery(id)
                        if (cur?.status != DeliveryStatus.DELIVERED && cur?.status != DeliveryStatus.FAILED) {
                            incidents.updateDelivery(id, DeliveryStatus.SENT)
                        }
                    }
                    ACTION_DELIVERED -> if (ok && lastPart) incidents.updateDelivery(id, DeliveryStatus.DELIVERED)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SENT = "com.dataveda.crashguard.SMS_SENT"
        const val ACTION_DELIVERED = "com.dataveda.crashguard.SMS_DELIVERED"
        const val EXTRA_DELIVERY_ID = "delivery_id"
        const val EXTRA_LAST_PART = "last_part"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlertRetryEntryPoint {
    fun incidents(): IncidentRepository
    fun dispatcher(): AlertDispatcher
}

/** Retries failed/queued SMS deliveries up to [MAX_SMS_ATTEMPTS] times. */
class AlertRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(applicationContext, AlertRetryEntryPoint::class.java)
        val pending = ep.incidents().pendingRetries(MAX_SMS_ATTEMPTS)
        pending.forEach { ep.dispatcher().sendSms(it.id) }
        return Result.success()
    }
}
