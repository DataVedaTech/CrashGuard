package com.dataveda.crashguard.core.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.R

enum class AppPermission(
    val permissions: List<String>,
    @StringRes val title: Int,
    @StringRes val reason: Int,
    @StringRes val limitation: Int,
    val critical: Boolean,
    val minSdk: Int = 1,
) {
    NOTIFICATIONS(
        listOf(Manifest.permission.POST_NOTIFICATIONS),
        R.string.perm_notifications, R.string.perm_notifications_why, R.string.perm_notifications_limit, true, 33,
    ),
    LOCATION(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        R.string.perm_location, R.string.perm_location_why, R.string.perm_location_limit, true,
    ),
    ACTIVITY(
        listOf(Manifest.permission.ACTIVITY_RECOGNITION),
        R.string.perm_activity, R.string.perm_activity_why, R.string.perm_activity_limit, true, 29,
    ),
    SMS(
        emptyList(),
        R.string.perm_sms, R.string.perm_sms_why, R.string.perm_sms_limit, false,
    ),
    CALL(
        emptyList(),
        R.string.perm_call, R.string.perm_call_why, R.string.perm_call_limit, false,
    ),
    BLUETOOTH(
        listOf(Manifest.permission.BLUETOOTH_CONNECT),
        R.string.perm_bluetooth, R.string.perm_bluetooth_why, R.string.perm_bluetooth_limit, false, 31,
    );

    val applicable: Boolean get() = Build.VERSION.SDK_INT >= minSdk

    fun isGranted(context: Context): Boolean =
        !applicable || permissions.isEmpty() || permissions.first().let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}

object PermissionUtils {
    fun canUseFullScreenIntent(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
        } else true

    fun appSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun fullScreenIntentSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else appSettingsIntent(context)

    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
