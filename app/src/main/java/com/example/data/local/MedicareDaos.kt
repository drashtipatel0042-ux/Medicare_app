package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY id DESC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?

    @Query("SELECT * FROM medicines WHERE remainingStock <= refillThreshold AND isActive = 1")
    fun getLowStockMedicines(): Flow<List<Medicine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("UPDATE medicines SET remainingStock = MAX(0, remainingStock - 1) WHERE id = :id")
    suspend fun decrementStock(id: Long)

    @Query("UPDATE medicines SET remainingStock = totalStock WHERE id = :id")
    suspend fun refillStock(id: Long)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE date = :dateString ORDER BY scheduledTime ASC")
    fun getLogsForDate(dateString: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE status = 'MISSED' ORDER BY id DESC")
    fun getMissedLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicineId = :medicineId ORDER BY id DESC LIMIT 20")
    fun getLogsForMedicine(medicineId: Long): Flow<List<DoseLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLog): Long

    @Update
    suspend fun updateLog(log: DoseLog)

    @Query("UPDATE dose_logs SET status = :newStatus, actionTimeMillis = :actionTime WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: DoseStatus, actionTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM dose_logs WHERE medicineId = :medicineId")
    suspend fun deleteLogsForMedicine(medicineId: Long)
}

@Dao
interface HealthVitalsDao {
    @Query("SELECT * FROM health_vitals ORDER BY timestamp DESC")
    fun getAllVitals(): Flow<List<HealthVitals>>

    @Query("SELECT * FROM health_vitals WHERE vitalType = :type ORDER BY timestamp DESC LIMIT 30")
    fun getVitalsByType(type: VitalType): Flow<List<HealthVitals>>

    @Query("SELECT * FROM health_vitals WHERE vitalType = 'WATER' AND dateString = :date ORDER BY timestamp DESC")
    fun getWaterLogsForDate(date: String): Flow<List<HealthVitals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVital(vital: HealthVitals): Long

    @Delete
    suspend fun deleteVital(vital: HealthVitals)
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY timestamp DESC")
    fun getAllPrescriptions(): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Long): Prescription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription): Long

    @Delete
    suspend fun deletePrescription(prescription: Prescription)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY appointmentDate ASC, appointmentTime ASC")
    fun getAllAppointments(): Flow<List<DoctorAppointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: DoctorAppointment): Long

    @Update
    suspend fun updateAppointment(appointment: DoctorAppointment)

    @Delete
    suspend fun deleteAppointment(appointment: DoctorAppointment)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimarySos DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}

@Dao
interface PatientProfileDao {
    @Query("SELECT * FROM patient_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<PatientProfile?>

    @Query("SELECT * FROM patient_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): PatientProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PatientProfile)
}
