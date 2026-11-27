package com.dataveda.crashguard.di

import android.content.Context
import androidx.room.Room
import com.dataveda.crashguard.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "crashguard.db").build()

    @Provides fun contactDao(db: AppDatabase) = db.contactDao()
    @Provides fun tripDao(db: AppDatabase) = db.tripDao()
    @Provides fun incidentDao(db: AppDatabase) = db.incidentDao()
    @Provides fun alertDeliveryDao(db: AppDatabase) = db.alertDeliveryDao()
    @Provides fun profileDao(db: AppDatabase) = db.profileDao()

    /** Long-lived scope for emergency workflows that must survive UI teardown. */
    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
