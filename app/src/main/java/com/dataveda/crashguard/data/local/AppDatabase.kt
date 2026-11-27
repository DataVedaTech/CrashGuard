package com.dataveda.crashguard.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import com.dataveda.crashguard.domain.models.DeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isPrimary DESC, createdAt ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isPrimary DESC, createdAt ASC")
    suspend fun getAll(): List<ContactEntity>

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE contacts SET isPrimary = (id = :id)")
    suspend fun setPrimary(id: String)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun count(): Int
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startedAt DESC LIMIT 200")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Query("UPDATE trips SET crashAlertCount = crashAlertCount + 1 WHERE id = :id")
    suspend fun incrementCrashAlerts(id: String)

    @Query("UPDATE trips SET falseAlarmCount = falseAlarmCount + 1 WHERE id = :id")
    suspend fun incrementFalseAlarms(id: String)

    @Query("UPDATE trips SET incidentCount = incidentCount + 1 WHERE id = :id")
    suspend fun incrementIncidents(id: String)

    @Query("UPDATE trips SET status = 'COMPLETED', endedAt = :now WHERE status = 'ACTIVE' AND startedAt < :now")
    suspend fun closeDangling(now: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY detectedAt DESC LIMIT 200")
    fun observeAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    fun observe(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun get(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity)

    @Update
    suspend fun update(incident: IncidentEntity)

    @Query("UPDATE incidents SET status = 'RESOLVED', resolvedAt = :now WHERE status IN ('ACTIVE','MONITORING') AND detectedAt < :now")
    suspend fun resolveDangling(now: Long)

    @Query("DELETE FROM incidents")
    suspend fun deleteAll()
}

@Dao
interface AlertDeliveryDao {
    @Query("SELECT * FROM alert_deliveries WHERE incidentId = :incidentId ORDER BY id ASC")
    fun observeForIncident(incidentId: String): Flow<List<AlertDeliveryEntity>>

    @Query("SELECT * FROM alert_deliveries WHERE id = :id")
    suspend fun get(id: Long): AlertDeliveryEntity?

    @Insert
    suspend fun insert(delivery: AlertDeliveryEntity): Long

    @Query("UPDATE alert_deliveries SET status = :status, lastError = :error, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DeliveryStatus, error: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE alert_deliveries SET attempts = attempts + 1, status = 'SENDING', updatedAt = :now WHERE id = :id")
    suspend fun markAttempt(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM alert_deliveries WHERE status IN ('QUEUED','FAILED') AND attempts < :maxAttempts")
    suspend fun pendingRetries(maxAttempts: Int): List<AlertDeliveryEntity>

    @Query("DELETE FROM alert_deliveries")
    suspend fun deleteAll()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Database(
    entities = [
        ContactEntity::class,
        TripEntity::class,
        IncidentEntity::class,
        AlertDeliveryEntity::class,
        ProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun tripDao(): TripDao
    abstract fun incidentDao(): IncidentDao
    abstract fun alertDeliveryDao(): AlertDeliveryDao
    abstract fun profileDao(): ProfileDao
}
