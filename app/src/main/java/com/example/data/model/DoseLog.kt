package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String, // "08:00 AM"
    val date: String, // "YYYY-MM-DD"
    val status: DoseStatus = DoseStatus.PENDING,
    val actionTimeMillis: Long = System.currentTimeMillis()
)

enum class DoseStatus(val labelEn: String, val labelGu: String) {
    PENDING("Upcoming", "બાકી છે"),
    TAKEN("Taken", "લીધી"),
    MISSED("Missed", "ચૂકી ગયા"),
    SNOOZED("Snoozed", "વિલંબિત")
}
