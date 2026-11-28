package com.dataveda.crashguard.core.emergency

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
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
import com.dataveda.crashguard.domain.models.AlertKind
import com.dataveda.crashguard.domain.models.DeliveryStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_SMS_ATTEMPTS = 3

/**
 * Play-Protect-Compliant Emergency Alert Dispatcher.
 * Persists every emergency delivery to local storage first (offline-safe), then dispatches via Android's
 * verified system SMS intent (`ACTION_SENDTO` / `smsto:`) without requiring restricted `SEND_SMS` manifest
 * permissions that trigger Google Play Protect Enhanced Fraud Protection blocks on sideloaded APKs.
 */
@Singleton
class AlertDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidents: IncidentRepository,
    private val settings: SettingsRepository,
) {
    fun canSendSms(smsEnabled: Boolean): Boolean =
        smsEnabled && context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    suspend fun dispatch(incidentId: String, contacts: List<ContactEntity>, kind: AlertKind, message: String) {
        val smsReady = canSendSms(settings.current().smsEnabled)
        val phones = contacts.map { it.phone }.filter { it.isNotBlank() }

        contacts.forEach { c ->
            val id = incidents.addDelivery(
                AlertDeliveryEntity(
                    incidentId = incidentId,
                    contactId = c.id,
                    contactName = c.name,
                    phone = c.phone,
                    kind = kind,
                    message = message,
                    status = if (smsReady && c.phone.isNotBlank()) DeliveryStatus.SENT else DeliveryStatus.MANUAL_REQUIRED,
                    attempts = 1,
                )
            )
            if (!smsReady || c.phone.isBlank()) {
                incidents.updateDelivery(id, DeliveryStatus.MANUAL_REQUIRED, "SMS disabled or empty phone")
            }
        }

        // Launch pre-addressed multi-recipient SMS composer for initial SOS, Test SOS, or Safe check-out
        if (smsReady && phones.isNotEmpty() && (kind == AlertKind.INITIAL || kind == AlertKind.TEST || kind == AlertKind.SAFE)) {
            runCatching {
                context.startActivity(composeIntent(phones, message))
            }.onFailure { e ->
                Log.w(TAG, "Unable to auto-open SMS composer intent", e)
            }
        }
    }

    suspend fun sendSms(deliveryId: Long) {
        val d = incidents.delivery(deliveryId) ?: return
        if (d.attempts >= MAX_SMS_ATTEMPTS || d.phone.isBlank()) {
            incidents.updateDelivery(deliveryId, DeliveryStatus.FAILED, "Max attempts or empty phone")
            return
        }
        incidents.markAttempt(deliveryId)
        runCatching {
            context.startActivity(composeIntent(listOf(d.phone), d.message))
            incidents.updateDelivery(deliveryId, DeliveryStatus.SENT)
        }.onFailure { e ->
            Log.w(TAG, "SMS compose dispatch failed for delivery $deliveryId", e)
            incidents.updateDelivery(deliveryId, DeliveryStatus.FAILED, e.javaClass.simpleName)
            if (d.attempts + 1 < MAX_SMS_ATTEMPTS) scheduleRetry(context)
        }
    }

    /** Opens the default SMS app pre-filled for all contacts (no restricted SEND_SMS permission needed). */
    fun composeIntent(phones: List<String>, message: String): Intent {
        val sep = if (Build.MANUFACTURER.equals("samsung", true)) "," else ";"
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phones.joinToString(sep)))
            .putExtra("sms_body", message)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

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
