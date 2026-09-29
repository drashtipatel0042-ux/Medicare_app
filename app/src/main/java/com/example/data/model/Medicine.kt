package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String, // e.g. "500 mg", "1 tablet", "10 ml"
    val type: MedicineType = MedicineType.TABLET,
    val foodTiming: FoodTiming = FoodTiming.AFTER_FOOD,
    val reminderTimes: List<String>, // e.g. ["08:00 AM", "01:00 PM", "08:00 PM"]
    val startDate: String, // "YYYY-MM-DD"
    val endDate: String = "", // empty if ongoing
    val repeatSchedule: RepeatSchedule = RepeatSchedule.DAILY,
    val totalStock: Int = 30,
    val remainingStock: Int = 30,
    val refillThreshold: Int = 5,
    val notes: String = "",
    val colorHex: String = "#0061A4",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MedicineType(val labelEn: String, val labelGu: String) {
    TABLET("Tablet", "ગોળી (Tablet)"),
    CAPSULE("Capsule", "કેપ્સ્યુલ (Capsule)"),
    SYRUP("Syrup", "સીરપ (Syrup)"),
    INJECTION("Injection", "ઇન્જેક્શન (Injection)"),
    DROPS("Eye/Ear Drops", "ટીપાં (Drops)"),
    INHALER("Inhaler", "ઇન્હેલર (Inhaler)"),
    OINTMENT("Ointment", "મલમ (Ointment)")
}

enum class FoodTiming(val labelEn: String, val labelGu: String) {
    BEFORE_FOOD("Before Food", "જમ્યા પહેલાં"),
    AFTER_FOOD("After Food", "જમ્યા પછી"),
    WITH_FOOD("With Food", "જમતી વખતે"),
    EMPTY_STOMACH("Empty Stomach", "ખાલી પેટે"),
    ANYTIME("Any Time", "કોઈપણ સમયે")
}

enum class RepeatSchedule(val labelEn: String, val labelGu: String) {
    DAILY("Every Day", "દરરોજ"),
    WEEKDAYS("Weekdays Only", "સોમ-શુક્ર"),
    ALTERNATE_DAYS("Every 2 Days", "એક દિવસ છોડીને"),
    AS_NEEDED("As Needed (SOS)", "જરૂર મુજબ")
}
