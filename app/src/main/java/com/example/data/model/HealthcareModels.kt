package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val doctorName: String,
    val hospitalClinic: String,
    val date: String,
    val diagnosis: String,
    val medicationsSummary: String,
    val notes: String = "",
    val imageUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class DoctorAppointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val doctorName: String,
    val specialty: String,
    val hospitalOrClinic: String,
    val appointmentDate: String, // "YYYY-MM-DD"
    val appointmentTime: String, // "10:30 AM"
    val contactPhone: String,
    val status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    val reasonForVisit: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppointmentStatus(val labelEn: String, val labelGu: String) {
    PENDING("Pending Confirmation", "પ્રતીક્ષામાં"),
    CONFIRMED("Confirmed", "પુષ્ટિ થયેલ"),
    COMPLETED("Completed", "પૂર્ણ થયેલ"),
    CANCELLED("Cancelled", "રદ કરેલ")
}

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val notifyOnMissedDose: Boolean = true,
    val isPrimarySos: Boolean = false
)

@Entity(tableName = "patient_profile")
data class PatientProfile(
    @PrimaryKey
    val id: Int = 1,
    val fullName: String = "Patient",
    val age: Int = 42,
    val gender: String = "Male",
    val bloodGroup: String = "B+",
    val heightCm: Double = 172.0,
    val weightKg: Double = 68.5,
    val emergencyPhone: String = "+91 98765 43210",
    val primaryDoctorName: String = "Dr. Amit Patel, M.D.",
    val primaryDoctorPhone: String = "+91 98250 12345",
    val allergies: String = "Penicillin, Peanuts",
    val chronicConditions: String = "Hypertension (Stage 1), Mild Asthma",
    val address: String = "Ahmedabad, Gujarat, India",
    val insuranceNumber: String = "MED-GTU-2026-9812",
    val emergencyIdToken: String = "MEDICARE-EMERGENCY-ID-9941"
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val languageCode: String = "en"
)
