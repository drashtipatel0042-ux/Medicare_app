package com.example.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MedicareApp
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("ReminderReceiver", "Received alarm intent action: $action")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Medicare:ReminderWakeLock"
        )
        wakeLock?.acquire(10 * 1000L) // 10 seconds timeout

        val pendingResult = goAsync()

        when (action) {
            ACTION_MEDICINE_ALARM -> {
                handleMedicineAlarm(context, intent)
                pendingResult.finish()
                wakeLock?.release()
            }
            ACTION_TAKE_DOSE -> {
                handleTakeDose(context, intent, pendingResult, wakeLock)
            }
            ACTION_SNOOZE_DOSE -> {
                handleSnoozeDose(context, intent)
                pendingResult.finish()
                wakeLock?.release()
            }
            else -> {
                pendingResult.finish()
                wakeLock?.release()
            }
        }
    }

    private fun handleMedicineAlarm(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "1 dose"
        val timing = intent.getStringExtra(EXTRA_MED_TIMING) ?: "As prescribed"
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: ""
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val notifId = (medId * 100).toInt() + 1

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "today_medicines")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Taken
        val takeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TAKE_DOSE
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_MED_NAME, medName)
            putExtra(EXTRA_MED_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_DOSE
            putExtra(EXTRA_MED_ID, medId)
            putExtra(EXTRA_MED_NAME, medName)
            putExtra(EXTRA_MED_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 20,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)

        val title = if (isSnooze) "⏰ Snooze Reminder: $medName" else "💊 Time to take: $medName"
        val contentText = "$dosage • $timing • Scheduled for $scheduledTime"

        val notificationBuilder = NotificationCompat.Builder(context, MedicareApp.CHANNEL_MEDICINE_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\nTake with water. Staying on schedule keeps you healthy."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true) // Heads-up when backgrounded or locked
            .addAction(android.R.drawable.checkbox_on_background, "Taken ✓", takePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, notificationBuilder.build())

        // Re-schedule recurring alarm for next day
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedicareApp.instance.database
                val med = db.medicineDao().getMedicineById(medId)
                if (med != null && med.isActive) {
                    AlarmScheduler.scheduleMedicineAlarms(context, med)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error re-scheduling alarm", e)
            }
        }
    }

    private fun handleTakeDose(
        context: Context,
        intent: Intent,
        pendingResult: PendingResult,
        wakeLock: PowerManager.WakeLock?
    ) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "1 dose"
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != 0) {
            notificationManager.cancel(notifId)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedicareApp.instance.database
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                db.doseLogDao().insertLog(
                    DoseLog(
                        medicineId = medId,
                        medicineName = medName,
                        dosage = dosage,
                        scheduledTime = scheduledTime,
                        date = todayStr,
                        status = DoseStatus.TAKEN,
                        actionTimeMillis = System.currentTimeMillis()
                    )
                )

                // Decrement stock
                db.medicineDao().decrementStock(medId)

                // Check for refill alert
                val med = db.medicineDao().getMedicineById(medId)
                if (med != null && med.remainingStock <= med.refillThreshold) {
                    showRefillAlert(context, med.name, med.remainingStock)
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error recording taken dose", e)
            } finally {
                pendingResult.finish()
                wakeLock?.release()
            }
        }
    }

    private fun handleSnoozeDose(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, 0L)
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: "1 dose"
        val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != 0) {
            notificationManager.cancel(notifId)
        }

        AlarmScheduler.scheduleSnoozeAlarm(context, medId, medName, dosage, scheduledTime, snoozeMinutes = 10)
    }

    private fun showRefillAlert(context: Context, medicineName: String, remaining: Int) {
        val notifId = (medicineName.hashCode()).coerceAtLeast(1000)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, MedicareApp.CHANNEL_REFILL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Refill Alert: $medicineName")
            .setContentText("Only $remaining doses left. Please refill your prescription soon.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, builder.build())
    }

    companion object {
        const val ACTION_MEDICINE_ALARM = "com.example.medicare.ACTION_MEDICINE_ALARM"
        const val ACTION_TAKE_DOSE = "com.example.medicare.ACTION_TAKE_DOSE"
        const val ACTION_SNOOZE_DOSE = "com.example.medicare.ACTION_SNOOZE_DOSE"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_MED_TIMING = "extra_med_timing"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }
}
