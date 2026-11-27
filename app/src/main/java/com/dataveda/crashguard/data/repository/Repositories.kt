package com.dataveda.crashguard.data.repository

import com.dataveda.crashguard.data.local.AlertDeliveryDao
import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.data.local.ContactDao
import com.dataveda.crashguard.data.local.ContactEntity
import com.dataveda.crashguard.data.local.CryptoManager
import com.dataveda.crashguard.data.local.IncidentDao
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.data.local.ProfileDao
import com.dataveda.crashguard.data.local.ProfileEntity
import com.dataveda.crashguard.data.local.TripDao
import com.dataveda.crashguard.data.local.TripEntity
import com.dataveda.crashguard.domain.models.EmergencyProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

const val MAX_CONTACTS = 5

@Singleton
class ContactRepository @Inject constructor(private val dao: ContactDao) {
    val contacts: Flow<List<ContactEntity>> = dao.observeAll()

    suspend fun all(): List<ContactEntity> = dao.getAll()

    /** Returns false if the 5-contact limit would be exceeded. */
    suspend fun save(id: String?, name: String, phone: String, primary: Boolean): Boolean {
        val existing = dao.getAll()
        if (id == null && existing.size >= MAX_CONTACTS) return false
        val contactId = id ?: UUID.randomUUID().toString()
        val createdAt = existing.firstOrNull { it.id == contactId }?.createdAt ?: System.currentTimeMillis()
        val makePrimary = primary || existing.isEmpty()
        dao.upsert(ContactEntity(contactId, name.trim(), normalizePhone(phone), makePrimary, createdAt = createdAt))
        if (makePrimary) dao.setPrimary(contactId)
        return true
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        val rest = dao.getAll()
        if (rest.isNotEmpty() && rest.none { it.isPrimary }) dao.setPrimary(rest.first().id)
    }

    suspend fun setPrimary(id: String) = dao.setPrimary(id)

    companion object {
        fun normalizePhone(raw: String): String {
            val trimmed = raw.trim()
            val plus = trimmed.startsWith("+")
            val digits = trimmed.filter { it.isDigit() }
            return if (plus) "+$digits" else digits
        }
    }
}

@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val crypto: CryptoManager,
) {
    val profile: Flow<EmergencyProfile> = dao.observe().map { it?.toDomain() ?: EmergencyProfile() }

    suspend fun get(): EmergencyProfile = dao.get()?.toDomain() ?: EmergencyProfile()

    suspend fun save(p: EmergencyProfile) {
        dao.upsert(
            ProfileEntity(
                name = p.name.trim(),
                phone = p.phone.trim(),
                bloodGroupEnc = crypto.encrypt(p.bloodGroup.trim()),
                allergiesEnc = crypto.encrypt(p.allergies.trim()),
                conditionsEnc = crypto.encrypt(p.conditions.trim()),
                medicationsEnc = crypto.encrypt(p.medications.trim()),
                insuranceEnc = crypto.encrypt(p.insurance.trim()),
                abhaEnc = crypto.encrypt(p.abhaId.trim()),
                vehicleType = p.vehicleType,
                shareBloodGroup = p.shareBloodGroup,
                shareAllergies = p.shareAllergies,
                shareConditions = p.shareConditions,
                shareMedications = p.shareMedications,
            )
        )
    }

    private fun ProfileEntity.toDomain() = EmergencyProfile(
        name = name,
        phone = phone,
        bloodGroup = crypto.decrypt(bloodGroupEnc),
        allergies = crypto.decrypt(allergiesEnc),
        conditions = crypto.decrypt(conditionsEnc),
        medications = crypto.decrypt(medicationsEnc),
        insurance = crypto.decrypt(insuranceEnc),
        abhaId = crypto.decrypt(abhaEnc),
        vehicleType = vehicleType,
        shareBloodGroup = shareBloodGroup,
        shareAllergies = shareAllergies,
        shareConditions = shareConditions,
        shareMedications = shareMedications,
    )
}

@Singleton
class TripRepository @Inject constructor(private val dao: TripDao) {
    val trips: Flow<List<TripEntity>> = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)
    suspend fun insert(trip: TripEntity) = dao.insert(trip)
    suspend fun update(trip: TripEntity) = dao.update(trip)
    suspend fun incrementCrashAlerts(id: String) = dao.incrementCrashAlerts(id)
    suspend fun incrementFalseAlarms(id: String) = dao.incrementFalseAlarms(id)
    suspend fun incrementIncidents(id: String) = dao.incrementIncidents(id)
    suspend fun closeDangling(before: Long = System.currentTimeMillis()) = dao.closeDangling(before)
    suspend fun deleteAll() = dao.deleteAll()
}

/** Local-first incident store. Every incident is persisted before any network/SMS action. */
@Singleton
class IncidentRepository @Inject constructor(
    private val dao: IncidentDao,
    private val deliveryDao: AlertDeliveryDao,
) {
    val incidents: Flow<List<IncidentEntity>> = dao.observeAll()
    fun observe(id: String): Flow<IncidentEntity?> = dao.observe(id)
    fun deliveries(incidentId: String): Flow<List<AlertDeliveryEntity>> = deliveryDao.observeForIncident(incidentId)

    suspend fun get(id: String) = dao.get(id)
    suspend fun create(incident: IncidentEntity) = dao.insert(incident)
    suspend fun update(incident: IncidentEntity) = dao.update(incident)

    suspend fun mutate(id: String, block: (IncidentEntity) -> IncidentEntity): IncidentEntity? {
        val current = dao.get(id) ?: return null
        val updated = block(current)
        dao.update(updated)
        return updated
    }

    suspend fun addDelivery(d: AlertDeliveryEntity): Long = deliveryDao.insert(d)
    suspend fun delivery(id: Long) = deliveryDao.get(id)
    suspend fun markAttempt(id: Long) = deliveryDao.markAttempt(id)
    suspend fun updateDelivery(id: Long, status: com.dataveda.crashguard.domain.models.DeliveryStatus, error: String? = null) =
        deliveryDao.updateStatus(id, status, error)
    suspend fun pendingRetries(maxAttempts: Int) = deliveryDao.pendingRetries(maxAttempts)

    suspend fun resolveDangling(before: Long = System.currentTimeMillis()) = dao.resolveDangling(before)

    suspend fun deleteAll() {
        deliveryDao.deleteAll()
        dao.deleteAll()
    }
}
