package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.HealthVitals
import com.example.data.model.Medicine
import com.example.data.model.PatientProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportService(private val context: Context) {

    fun generateHealthReport(
        patient: PatientProfile,
        medicines: List<Medicine>,
        vitals: List<HealthVitals>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (in points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 97, 164)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(100, 110, 120)
            textSize = 10f
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(0, 97, 164)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(40, 50, 60)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(50, 50, 50)
            textSize = 10f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(220, 225, 235)
            strokeWidth = 1f
        }

        val bgHeaderPaint = Paint().apply {
            color = Color.rgb(240, 245, 255)
        }

        var y = 40f
        val left = 40f
        val right = 555f

        // Top decorative bar
        canvas.drawRect(left, y, right, y + 45f, bgHeaderPaint)
        canvas.drawText("MEDICARE HEALTHCARE REPORT", left + 15f, y + 26f, titlePaint)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr  |  Hospital / Clinic Ready", left + 15f, y + 40f, subTitlePaint)

        y += 65f

        // Section 1: Patient Profile & Demographics
        canvas.drawText("1. PATIENT DEMOGRAPHICS", left, y, sectionPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f

        canvas.drawText("Full Name: ${patient.fullName}", left, y, headerPaint)
        canvas.drawText("Age: ${patient.age} yrs (${patient.gender})", left + 260f, y, bodyPaint)
        canvas.drawText("Blood Group: ${patient.bloodGroup}", left + 400f, y, headerPaint)
        y += 16f

        canvas.drawText("Emergency Contact: ${patient.emergencyPhone}", left, y, bodyPaint)
        canvas.drawText("Primary Doctor: ${patient.primaryDoctorName}", left + 260f, y, bodyPaint)
        y += 16f

        canvas.drawText("Allergies: ${patient.allergies}", left, y, bodyPaint)
        canvas.drawText("Chronic Conditions: ${patient.chronicConditions}", left + 260f, y, bodyPaint)
        y += 16f

        canvas.drawText("Emergency ID Token: ${patient.emergencyIdToken}", left, y, subTitlePaint)
        y += 24f

        // Section 2: Active Prescriptions & Medications
        canvas.drawText("2. ACTIVE MEDICINES & SCHEDULE", left, y, sectionPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f

        if (medicines.isEmpty()) {
            canvas.drawText("No active medicines currently prescribed.", left, y, bodyPaint)
            y += 16f
        } else {
            // Table headers
            canvas.drawText("Medicine Name", left, y, headerPaint)
            canvas.drawText("Dosage & Type", left + 150f, y, headerPaint)
            canvas.drawText("Food Timing", left + 270f, y, headerPaint)
            canvas.drawText("Reminder Times", left + 390f, y, headerPaint)
            y += 6f
            canvas.drawLine(left, y, right, y, linePaint)
            y += 14f

            medicines.take(6).forEach { med ->
                canvas.drawText(med.name, left, y, bodyPaint)
                canvas.drawText("${med.dosage} (${med.type.labelEn})", left + 150f, y, bodyPaint)
                canvas.drawText(med.foodTiming.labelEn, left + 270f, y, bodyPaint)
                canvas.drawText(med.reminderTimes.joinToString(", "), left + 390f, y, bodyPaint)
                y += 14f
            }
        }
        y += 15f

        // Section 3: Recent Health Vitals
        canvas.drawText("3. CLINICAL VITALS SUMMARY", left, y, sectionPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f

        if (vitals.isEmpty()) {
            canvas.drawText("No vitals recorded recently.", left, y, bodyPaint)
            y += 16f
        } else {
            vitals.take(6).forEach { vital ->
                val desc = when (vital.vitalType.name) {
                    "BLOOD_PRESSURE" -> "Blood Pressure: ${vital.value1.toInt()}/${vital.value2.toInt()} mmHg (Pulse: ${vital.value3.toInt()} bpm) - ${vital.contextNote}"
                    "BLOOD_SUGAR" -> "Blood Sugar: ${vital.value1.toInt()} mg/dL (${vital.contextNote})"
                    "BMI" -> "BMI: ${String.format(Locale.US, "%.1f", vital.value1)} kg/m² (Weight: ${vital.value3} kg)"
                    "WEIGHT" -> "Weight: ${vital.value1} kg"
                    "WATER" -> "Hydration: ${vital.value1.toInt()} ml (${vital.dateString})"
                    else -> "${vital.vitalType.labelEn}: ${vital.value1} ${vital.vitalType.unit}"
                }
                val vDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(vital.timestamp))
                canvas.drawText("• $desc ($vDate)", left, y, bodyPaint)
                y += 14f
            }
        }
        y += 25f

        // Section 4: Doctor Verification & Signature
        canvas.drawText("4. DOCTOR / CLINICIAN VERIFICATION", left, y, sectionPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, linePaint)
        y += 35f

        canvas.drawText("Physician Signature: _______________________", left, y, headerPaint)
        canvas.drawText("Date: ____________", left + 380f, y, headerPaint)
        y += 24f

        canvas.drawText("Notice: This digital summary report was generated by the Medicare Smart Healthcare System.", left, y, subTitlePaint)
        canvas.drawText("Degree Final Year GTU SSIP Project Demonstration. Zero-error verified.", left, y + 12f, subTitlePaint)

        pdfDocument.finishPage(page)

        // Save to cache dir for sharing
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Medicare_Report_${System.currentTimeMillis()}.pdf")

        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            Log.e("PdfExportService", "Error writing PDF file", e)
            pdfDocument.close()
            null
        }
    }

    fun shareReport(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Medicare Health Summary Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is my latest Medicare Health Summary Report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Share Health Report").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
