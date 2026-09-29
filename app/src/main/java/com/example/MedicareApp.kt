package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.MedicareDatabase
import com.example.data.repository.MedicareRepository

class MedicareApp : Application() {

    lateinit var database: MedicareDatabase
        private set
    lateinit var repository: MedicareRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Safe Firebase initialization if resources are provided
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.d("MedicareApp", "Firebase default init skipped: ${e.message}")
        }

        database = MedicareDatabase.getDatabase(this)
        repository = MedicareRepository(this, database)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val reminderChannel = NotificationChannel(
                CHANNEL_MEDICINE_REMINDERS,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts to take scheduled medicines on time"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val refillChannel = NotificationChannel(
                CHANNEL_REFILL_ALERTS,
                "Refill & Health Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Low stock medicine alerts and health updates"
            }

            val sosChannel = NotificationChannel(
                CHANNEL_SOS,
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts and family SOS broadcasts"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(refillChannel)
            notificationManager.createNotificationChannel(sosChannel)
        }
    }

    companion object {
        const val CHANNEL_MEDICINE_REMINDERS = "medicare_medicine_reminders"
        const val CHANNEL_REFILL_ALERTS = "medicare_refill_alerts"
        const val CHANNEL_SOS = "medicare_emergency_sos"

        @JvmStatic
        lateinit var instance: MedicareApp
            private set
    }
}
