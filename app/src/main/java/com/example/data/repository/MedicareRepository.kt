package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.MedicareDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicareRepository(
    private val context: Context,
    private val database: MedicareDatabase
) {
    private val medicineDao = database.medicineDao()
    private val doseLogDao = database.doseLogDao()
    private val vitalsDao = database.healthVitalsDao()
    private val prescriptionDao = database.prescriptionDao()
    private val appointmentDao = database.appointmentDao()
    private val contactDao = database.emergencyContactDao()
    private val profileDao = database.patientProfileDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedDefaultDataIfEmpty()
        }
    }

    // Medicines
    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<Medicine>> = medicineDao.getActiveMedicines()
    val lowStockMedicines: Flow<List<Medicine>> = medicineDao.getLowStockMedicines()

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun saveMedicine(medicine: Medicine): Long {
        val id = medicineDao.insertMedicine(medicine)
        val saved = medicine.copy(id = if (medicine.id == 0L) id else medicine.id)
        AlarmScheduler.scheduleMedicineAlarms(context, saved)
        return id
    }

    suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
        AlarmScheduler.cancelMedicineAlarms(context, medicine.id)
        if (medicine.isActive) {
            AlarmScheduler.scheduleMedicineAlarms(context, medicine)
        }
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        AlarmScheduler.cancelMedicineAlarms(context, medicine.id)
        medicineDao.deleteMedicine(medicine)
        doseLogDao.deleteLogsForMedicine(medicine.id)
    }

    suspend fun refillMedicineStock(medicineId: Long) {
        medicineDao.refillStock(medicineId)
    }

    // Dose Logs
    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()
    val missedLogs: Flow<List<DoseLog>> = doseLogDao.getMissedLogs()

    fun getLogsForDate(dateString: String): Flow<List<DoseLog>> = doseLogDao.getLogsForDate(dateString)

    suspend fun recordDoseAction(logId: Long, status: DoseStatus, medicineId: Long) {
        doseLogDao.updateStatus(logId, status)
        if (status == DoseStatus.TAKEN) {
            medicineDao.decrementStock(medicineId)
        }
    }

    suspend fun addDoseLog(log: DoseLog): Long {
        return doseLogDao.insertLog(log)
    }

    // Vitals
    val allVitals: Flow<List<HealthVitals>> = vitalsDao.getAllVitals()
    fun getVitalsByType(type: VitalType): Flow<List<HealthVitals>> = vitalsDao.getVitalsByType(type)
    fun getWaterLogsForDate(date: String): Flow<List<HealthVitals>> = vitalsDao.getWaterLogsForDate(date)

    suspend fun recordVital(vital: HealthVitals): Long = vitalsDao.insertVital(vital)
    suspend fun deleteVital(vital: HealthVitals) = vitalsDao.deleteVital(vital)

    // Prescriptions
    val allPrescriptions: Flow<List<Prescription>> = prescriptionDao.getAllPrescriptions()
    suspend fun savePrescription(prescription: Prescription): Long = prescriptionDao.insertPrescription(prescription)
    suspend fun deletePrescription(prescription: Prescription) = prescriptionDao.deletePrescription(prescription)

    // Appointments
    val allAppointments: Flow<List<DoctorAppointment>> = appointmentDao.getAllAppointments()
    suspend fun bookAppointment(appointment: DoctorAppointment): Long = appointmentDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: DoctorAppointment) = appointmentDao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: DoctorAppointment) = appointmentDao.deleteAppointment(appointment)

    // Emergency Contacts
    val emergencyContacts: Flow<List<EmergencyContact>> = contactDao.getAllContacts()
    suspend fun saveContact(contact: EmergencyContact): Long = contactDao.insertContact(contact)
    suspend fun deleteContact(contact: EmergencyContact) = contactDao.deleteContact(contact)

    // Patient Profile
    val patientProfile: Flow<PatientProfile?> = profileDao.getProfile()
    suspend fun savePatientProfile(profile: PatientProfile) = profileDao.saveProfile(profile)

    private suspend fun seedDefaultDataIfEmpty() {
        val existingMeds = medicineDao.getAllMedicines().firstOrNull()
        if (existingMeds.isNullOrEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val m1 = Medicine(
                name = "Metformin HCl",
                dosage = "500 mg",
                type = MedicineType.TABLET,
                foodTiming = FoodTiming.AFTER_FOOD,
                reminderTimes = listOf("08:30 AM", "08:30 PM"),
                startDate = todayStr,
                repeatSchedule = RepeatSchedule.DAILY,
                totalStock = 30,
                remainingStock = 18,
                refillThreshold = 5,
                notes = "Take with a glass of water after meals.",
                colorHex = "#0061A4"
            )
            val m2 = Medicine(
                name = "Amlodipine Besylate",
                dosage = "5 mg",
                type = MedicineType.TABLET,
                foodTiming = FoodTiming.BEFORE_FOOD,
                reminderTimes = listOf("09:00 AM"),
                startDate = todayStr,
                repeatSchedule = RepeatSchedule.DAILY,
                totalStock = 20,
                remainingStock = 4, // Triggers refill badge!
                refillThreshold = 5,
                notes = "Maintain blood pressure control.",
                colorHex = "#006A6A"
            )
            val m3 = Medicine(
                name = "Vitamin D3 & Calcium",
                dosage = "1000 IU",
                type = MedicineType.CAPSULE,
                foodTiming = FoodTiming.WITH_FOOD,
                reminderTimes = listOf("01:00 PM"),
                startDate = todayStr,
                repeatSchedule = RepeatSchedule.DAILY,
                totalStock = 60,
                remainingStock = 45,
                refillThreshold = 10,
                notes = "Bone strengthening supplement.",
                colorHex = "#805600"
            )

            val id1 = medicineDao.insertMedicine(m1)
            val id2 = medicineDao.insertMedicine(m2)
            val id3 = medicineDao.insertMedicine(m3)

            // Seed sample dose logs for today
            doseLogDao.insertLog(
                DoseLog(
                    medicineId = id1,
                    medicineName = "Metformin HCl",
                    dosage = "500 mg",
                    scheduledTime = "08:30 AM",
                    date = todayStr,
                    status = DoseStatus.TAKEN
                )
            )
            doseLogDao.insertLog(
                DoseLog(
                    medicineId = id2,
                    medicineName = "Amlodipine Besylate",
                    dosage = "5 mg",
                    scheduledTime = "09:00 AM",
                    date = todayStr,
                    status = DoseStatus.PENDING
                )
            )
            doseLogDao.insertLog(
                DoseLog(
                    medicineId = id3,
                    medicineName = "Vitamin D3 & Calcium",
                    dosage = "1000 IU",
                    scheduledTime = "01:00 PM",
                    date = todayStr,
                    status = DoseStatus.PENDING
                )
            )

            // Seed Vitals
            vitalsDao.insertVital(
                HealthVitals(
                    vitalType = VitalType.BLOOD_PRESSURE,
                    value1 = 122.0, // Systolic
                    value2 = 82.0,  // Diastolic
                    value3 = 74.0,  // Pulse
                    contextNote = "Resting morning check",
                    dateString = todayStr
                )
            )
            vitalsDao.insertVital(
                HealthVitals(
                    vitalType = VitalType.BLOOD_SUGAR,
                    value1 = 98.0,
                    contextNote = "Fasting glucose",
                    dateString = todayStr
                )
            )
            vitalsDao.insertVital(
                HealthVitals(
                    vitalType = VitalType.BMI,
                    value1 = 23.1, // BMI
                    value2 = 172.0, // Height cm
                    value3 = 68.5,  // Weight kg
                    contextNote = "Normal healthy range",
                    dateString = todayStr
                )
            )
            vitalsDao.insertVital(
                HealthVitals(
                    vitalType = VitalType.WATER,
                    value1 = 1500.0, // ml
                    contextNote = "6 glasses taken",
                    dateString = todayStr
                )
            )

            // Seed Emergency Contacts
            contactDao.insertContact(
                EmergencyContact(
                    name = "Priya Sharma (Spouse)",
                    relationship = "Wife",
                    phoneNumber = "+91 98765 11223",
                    notifyOnMissedDose = true,
                    isPrimarySos = true
                )
            )
            contactDao.insertContact(
                EmergencyContact(
                    name = "Ramesh Sharma (Brother)",
                    relationship = "Brother",
                    phoneNumber = "+91 98250 99887",
                    notifyOnMissedDose = true,
                    isPrimarySos = false
                )
            )

            // Seed Patient Profile with dynamic user session name
            val sessionName = com.example.auth.UserSessionManager.getUserName(context)
            val sessionPhone = com.example.auth.UserSessionManager.getMobileNumber(context)

            profileDao.saveProfile(
                PatientProfile(
                    fullName = if (sessionName.isNotBlank() && sessionName != "Patient") sessionName else "Patient",
                    age = 42,
                    gender = "Male",
                    bloodGroup = "B+",
                    heightCm = 172.0,
                    weightKg = 68.5,
                    emergencyPhone = if (sessionPhone.isNotBlank()) sessionPhone else "+91 98765 11223",
                    primaryDoctorName = "Dr. Hiren Desai, M.D.",
                    primaryDoctorPhone = "+91 98250 55443",
                    allergies = "Penicillin, Pollen",
                    chronicConditions = "Hypertension (Mild)",
                    address = "Navrangpura, Ahmedabad, Gujarat",
                    insuranceNumber = "GTU-MED-2026-7889"
                )
            )

            // Seed Appointments
            appointmentDao.insertAppointment(
                DoctorAppointment(
                    doctorName = "Dr. Hiren Desai",
                    specialty = "Senior Cardiologist",
                    hospitalOrClinic = "Apollo City Hospital, Ahmedabad",
                    appointmentDate = todayStr,
                    appointmentTime = "04:30 PM",
                    contactPhone = "+91 98250 55443",
                    status = AppointmentStatus.CONFIRMED,
                    reasonForVisit = "Quarterly BP & Heart checkup"
                )
            )

            // Seed Prescription
            prescriptionDao.insertPrescription(
                Prescription(
                    title = "Hypertension & Diabetic Care Rx",
                    doctorName = "Dr. Hiren Desai, M.D.",
                    hospitalClinic = "Apollo City Hospital",
                    date = todayStr,
                    diagnosis = "Essential Hypertension & Pre-diabetes monitoring",
                    medicationsSummary = "Metformin 500mg (1-0-1), Amlodipine 5mg (1-0-0), Multivitamin (0-1-0)",
                    notes = "Maintain low salt diet. Recheck BP in 3 months."
                )
            )
        }
    }
}
