package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_vitals")
data class HealthVitals(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vitalType: VitalType,
    val value1: Double, // Systolic BP / BMI / Glucose mg/dL / Weight kg / Water ml
    val value2: Double = 0.0, // Diastolic BP / Height cm
    val value3: Double = 0.0, // Pulse bpm
    val contextNote: String = "", // e.g. "Fasting", "After lunch", "Resting"
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = "" // "YYYY-MM-DD"
)

enum class VitalType(val labelEn: String, val labelGu: String, val unit: String) {
    BLOOD_PRESSURE("Blood Pressure", "બ્લડ પ્રેશર (BP)", "mmHg"),
    BLOOD_SUGAR("Blood Sugar", "સુગર (ગ્લુકોઝ)", "mg/dL"),
    BMI("BMI & Weight", "શરીરનું વજન અને BMI", "kg/m²"),
    WEIGHT("Weight", "વજન", "kg"),
    WATER("Water Intake", "પાણીનું પ્રમાણ", "ml")
}
