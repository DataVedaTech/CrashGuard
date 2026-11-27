package com.dataveda.crashguard.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "crashguard_settings")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val protectionEnabled: Boolean = false,
    val smsEnabled: Boolean = false,
    val autoCallEnabled: Boolean = false,
    val developerMode: Boolean = false,
    val countdownSeconds: Int = 45,
    val impactThresholdG: Float = 4.5f,
    val minSpeedBeforeKmh: Float = 15f,
    val speedDropKmh: Float = 10f,
    val rotationThresholdDegPerSec: Float = 200f,
    val liveSharingMinutes: Int = 15,
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PROTECTION = booleanPreferencesKey("protection_enabled")
        val SMS = booleanPreferencesKey("sms_enabled")
        val AUTO_CALL = booleanPreferencesKey("auto_call_enabled")
        val DEV = booleanPreferencesKey("developer_mode")
        val COUNTDOWN = intPreferencesKey("countdown_seconds")
        val IMPACT_G = floatPreferencesKey("impact_threshold_g")
        val MIN_SPEED = floatPreferencesKey("min_speed_before_kmh")
        val SPEED_DROP = floatPreferencesKey("speed_drop_kmh")
        val ROTATION = floatPreferencesKey("rotation_threshold_dps")
        val SHARE_MIN = intPreferencesKey("live_sharing_minutes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings() = AppSettings(
        onboardingDone = this[Keys.ONBOARDING] ?: false,
        protectionEnabled = this[Keys.PROTECTION] ?: false,
        smsEnabled = this[Keys.SMS] ?: false,
        autoCallEnabled = this[Keys.AUTO_CALL] ?: false,
        developerMode = this[Keys.DEV] ?: false,
        countdownSeconds = this[Keys.COUNTDOWN] ?: 45,
        impactThresholdG = this[Keys.IMPACT_G] ?: 4.5f,
        minSpeedBeforeKmh = this[Keys.MIN_SPEED] ?: 15f,
        speedDropKmh = this[Keys.SPEED_DROP] ?: 10f,
        rotationThresholdDegPerSec = this[Keys.ROTATION] ?: 200f,
        liveSharingMinutes = this[Keys.SHARE_MIN] ?: 15,
    )

    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING] = v }
    suspend fun setProtectionEnabled(v: Boolean) = context.dataStore.edit { it[Keys.PROTECTION] = v }
    suspend fun setSmsEnabled(v: Boolean) = context.dataStore.edit { it[Keys.SMS] = v }
    suspend fun setAutoCallEnabled(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_CALL] = v }
    suspend fun setDeveloperMode(v: Boolean) = context.dataStore.edit { it[Keys.DEV] = v }
    suspend fun setCountdownSeconds(v: Int) = context.dataStore.edit { it[Keys.COUNTDOWN] = v.coerceIn(10, 120) }
    suspend fun setImpactThreshold(v: Float) = context.dataStore.edit { it[Keys.IMPACT_G] = v }
}
