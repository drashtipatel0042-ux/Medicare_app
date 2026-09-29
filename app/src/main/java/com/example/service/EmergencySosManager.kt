package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class EmergencySosManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var isSirenActive = false
    private var isTorchActive = false
    private var cameraId: String? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    init {
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Error obtaining cameraId", e)
        }
    }

    fun startSirenAndAlert() {
        if (isSirenActive) return
        isSirenActive = true

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 15000)
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Error starting tone", e)
        }

        // Vibrate in emergency pattern
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 400, 200, 400, 200, 800)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 800), 0)
            }
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Error vibrating", e)
        }

        // Toggle torch
        toggleTorch(true)
    }

    fun stopSirenAndAlert() {
        isSirenActive = false
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Error stopping tone", e)
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Error cancelling vibration", e)
        }

        toggleTorch(false)
    }

    fun dialEmergencyContact(phoneNumber: String) {
        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun sendSosSms(phoneNumber: String, patientName: String, locationOrAddress: String) {
        val message = "🚨 EMERGENCY SOS ALERT! 🚨\n" +
                "Patient $patientName requires immediate medical assistance.\n" +
                "Location: $locationOrAddress\n" +
                "Sent automatically via Medicare Healthcare App."

        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(smsIntent)
    }

    private fun toggleTorch(enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraId != null) {
            try {
                cameraManager?.setTorchMode(cameraId!!, enable)
                isTorchActive = enable
            } catch (e: Exception) {
                Log.e("EmergencySosManager", "Torch mode error", e)
            }
        }
    }
}
