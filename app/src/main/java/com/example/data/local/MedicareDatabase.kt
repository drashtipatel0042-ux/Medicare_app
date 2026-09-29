package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        Medicine::class,
        DoseLog::class,
        HealthVitals::class,
        Prescription::class,
        DoctorAppointment::class,
        EmergencyContact::class,
        PatientProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedicareDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun healthVitalsDao(): HealthVitalsDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun patientProfileDao(): PatientProfileDao

    companion object {
        @Volatile
        private var INSTANCE: MedicareDatabase? = null

        fun getDatabase(context: Context): MedicareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicareDatabase::class.java,
                    "medicare_health.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
