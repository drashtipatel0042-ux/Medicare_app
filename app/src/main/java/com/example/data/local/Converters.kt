package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AppointmentStatus
import com.example.data.model.DoseStatus
import com.example.data.model.FoodTiming
import com.example.data.model.MedicineType
import com.example.data.model.RepeatSchedule
import com.example.data.model.VitalType

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(";") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromMedicineType(value: MedicineType?): String = value?.name ?: MedicineType.TABLET.name

    @TypeConverter
    fun toMedicineType(value: String?): MedicineType =
        try { MedicineType.valueOf(value ?: MedicineType.TABLET.name) } catch (e: Exception) { MedicineType.TABLET }

    @TypeConverter
    fun fromFoodTiming(value: FoodTiming?): String = value?.name ?: FoodTiming.AFTER_FOOD.name

    @TypeConverter
    fun toFoodTiming(value: String?): FoodTiming =
        try { FoodTiming.valueOf(value ?: FoodTiming.AFTER_FOOD.name) } catch (e: Exception) { FoodTiming.AFTER_FOOD }

    @TypeConverter
    fun fromRepeatSchedule(value: RepeatSchedule?): String = value?.name ?: RepeatSchedule.DAILY.name

    @TypeConverter
    fun toRepeatSchedule(value: String?): RepeatSchedule =
        try { RepeatSchedule.valueOf(value ?: RepeatSchedule.DAILY.name) } catch (e: Exception) { RepeatSchedule.DAILY }

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus?): String = value?.name ?: DoseStatus.PENDING.name

    @TypeConverter
    fun toDoseStatus(value: String?): DoseStatus =
        try { DoseStatus.valueOf(value ?: DoseStatus.PENDING.name) } catch (e: Exception) { DoseStatus.PENDING }

    @TypeConverter
    fun fromVitalType(value: VitalType?): String = value?.name ?: VitalType.BLOOD_PRESSURE.name

    @TypeConverter
    fun toVitalType(value: String?): VitalType =
        try { VitalType.valueOf(value ?: VitalType.BLOOD_PRESSURE.name) } catch (e: Exception) { VitalType.BLOOD_PRESSURE }

    @TypeConverter
    fun fromAppointmentStatus(value: AppointmentStatus?): String = value?.name ?: AppointmentStatus.CONFIRMED.name

    @TypeConverter
    fun toAppointmentStatus(value: String?): AppointmentStatus =
        try { AppointmentStatus.valueOf(value ?: AppointmentStatus.CONFIRMED.name) } catch (e: Exception) { AppointmentStatus.CONFIRMED }
}
