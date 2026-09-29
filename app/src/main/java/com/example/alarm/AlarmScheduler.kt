package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.Medicine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun scheduleMedicineAlarms(context: Context, medicine: Medicine) {
        if (!medicine.isActive) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        medicine.reminderTimes.forEachIndexed { index, timeStr ->
            val triggerMillis = computeNextTriggerTime(timeStr)
            val requestCode = generateRequestCode(medicine.id, index)

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_MEDICINE_ALARM
                putExtra(ReminderReceiver.EXTRA_MED_ID, medicine.id)
                putExtra(ReminderReceiver.EXTRA_MED_NAME, medicine.name)
                putExtra(ReminderReceiver.EXTRA_MED_DOSAGE, medicine.dosage)
                putExtra(ReminderReceiver.EXTRA_MED_TIMING, medicine.foodTiming.labelEn)
                putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, timeStr)
                putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactOrFallback(context, alarmManager, triggerMillis, pendingIntent, medicine.name)
        }
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = 999999
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MEDICINE_ALARM
            putExtra(ReminderReceiver.EXTRA_MED_ID, 999L)
            putExtra(ReminderReceiver.EXTRA_MED_NAME, "Test Medicine (Paracetamol 500mg)")
            putExtra(ReminderReceiver.EXTRA_MED_DOSAGE, "1 Tablet")
            putExtra(ReminderReceiver.EXTRA_MED_TIMING, "After Food")
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, "Now")
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(context, alarmManager, triggerMillis, pendingIntent, "Test Medicine Alarm")
    }

    fun cancelMedicineAlarms(context: Context, medicineId: Long, reminderTimesCount: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (index in 0 until reminderTimesCount) {
            val requestCode = generateRequestCode(medicineId, index)
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        snoozeMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = (medicineId * 1000 + 999).toInt()
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MEDICINE_ALARM
            putExtra(ReminderReceiver.EXTRA_MED_ID, medicineId)
            putExtra(ReminderReceiver.EXTRA_MED_NAME, medicineName)
            putExtra(ReminderReceiver.EXTRA_MED_DOSAGE, dosage)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
            putExtra(ReminderReceiver.EXTRA_IS_SNOOZE, true)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(context, alarmManager, triggerMillis, pendingIntent, "$medicineName (Snooze)")
    }

    private fun scheduleExactOrFallback(
        context: Context,
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent,
        tag: String
    ) {
        try {
            // Intent to show when alarm fires or user clicks alarm clock indicator in status bar
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "today_medicines")
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                pendingIntent.hashCode(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canScheduleExactAlarms(context)) {
                // setAlarmClock is the highest priority exact alarm in Android, wakes up device in Doze mode
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("AlarmScheduler", "Scheduled exact AlarmClock for $tag at $triggerMillis")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                Log.d("AlarmScheduler", "Scheduled exact allow while idle for $tag at $triggerMillis")
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w("AlarmScheduler", "Exact alarm not allowed, falling back to setAndAllowWhileIdle", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling alarm for $tag", e)
        }
    }

    private fun computeNextTriggerTime(timeStr: String): Long {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("hh:mm a", Locale.US)
        try {
            val date = format.parse(timeStr.trim())
            if (date != null) {
                val timeCal = Calendar.getInstance().apply { time = date }
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // If scheduled time has already passed today, schedule for tomorrow
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Parse time error for $timeStr", e)
        }
        return System.currentTimeMillis() + 60 * 1000 // fallback 1 minute
    }

    private fun generateRequestCode(medicineId: Long, index: Int): Int {
        return (medicineId * 100 + index).toInt()
    }
}
