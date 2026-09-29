package com.example.service

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiHealthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getHealthAdvice(userPrompt: String, isGujarati: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalClinicalResponse(userPrompt, isGujarati)
        }

        val systemPrompt = if (isGujarati) {
            "તમે 'મેડિકેર' (Medicare) એઆઈ સ્માર્ટ હેલ્થ આસિસ્ટન્ટ છો. દર્દીઓને દવાઓ, લક્ષણો, જીવનશૈલી અને સ્વાસ્થ્ય અંગે ગુજરાતી ભાષામાં સચોટ, નમ્ર અને સલામત સલાહ આપો. હંમેશા ગંભીર સ્થિતિમાં ડૉક્ટરની સલાહ લેવાનું જણાવો."
        } else {
            "You are Medicare AI, an intelligent, empathetic medical and healthcare assistant. Help the patient with medicine schedules, symptom triage, healthy diet, BMI, blood pressure, blood sugar management, and first aid. Always maintain a calm, professional tone with a medical disclaimer for emergencies."
        }

        val fullPrompt = "$systemPrompt\n\nPatient Query: $userPrompt"

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()
                val partObj = JSONObject().put("text", fullPrompt)
                partsArray.put(partObj)
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiHealthService", "Gemini HTTP error ${response.code}: ${response.message}")
                    return@withContext getLocalClinicalResponse(userPrompt, isGujarati)
                }

                val responseBodyStr = response.body?.string() ?: return@withContext getLocalClinicalResponse(userPrompt, isGujarati)
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiHealthService", "Gemini API call failed", e)
        }

        return@withContext getLocalClinicalResponse(userPrompt, isGujarati)
    }

    private fun getLocalClinicalResponse(prompt: String, isGujarati: Boolean): String {
        val lower = prompt.lowercase()
        return if (isGujarati) {
            when {
                lower.contains("bp") || lower.contains("પ્રેશર") || lower.contains("blood pressure") ->
                    "સામાન્ય બ્લડ પ્રેશર ૧૨૦/૮૦ mmHg હોવું જોઈએ. જો તમારું BP ૧૪૦/૯૦ થી વધુ હોય, તો મીઠાનું પ્રમાણ ઘટાડો, દરરોજ ૩૦ મિનિટ કસરત કરો અને તમારા નિયત સમયે દવા લેવાનું ભૂલશો નહીં."
                lower.contains("સુગર") || lower.contains("sugar") || lower.contains("diabetes") ->
                    "ખાલી પેટે સામાન્ય સુગર ૭૦-૧૦૦ mg/dL અને જમ્યા પછી ૧૪૦ mg/dL થી ઓછું હોવું હિતાવહ છે. નિયમિત હળવી કસરત કરો અને વધુ પડતા કાર્બોહાઇડ્રેટ્સ ટાળો."
                lower.contains("માથું") || lower.contains("headache") || lower.contains("તાવ") || lower.contains("fever") ->
                    "પૂરતું પાણી પીવો અને આરામ કરો. જો તાવ ૧૦૧°F કરતાં વધુ રહે અથવા માથાનો દુખાવો સતત રહે તો તરત જ ડૉક્ટરની સલાહ લો."
                lower.contains("પાણી") || lower.contains("water") ->
                    "સ્વસ્થ પુખ્ત વ્યક્તિએ દિવસમાં ૨.૫ થી ૩ લિટર (૮-૧૦ ગ્લાસ) શુદ્ધ પાણી પીવું જોઈએ. તે કિડની અને શરીરની કાર્યક્ષમતા જાળવે છે."
                else ->
                    "નમસ્તે! હું મેડિકેર AI હેલ્થ સહાયક છું. તમારી દવાઓ સમયસર લો, નિયમિત પાણી પીવો અને જો કોઈ ગંભીર તકલીફ જણાય તો હોસ્પિટલમાં ડૉક્ટરનો સંપર્ક કરો."
            }
        } else {
            when {
                lower.contains("bp") || lower.contains("pressure") || lower.contains("hypertension") ->
                    "Normal Blood Pressure is around 120/80 mmHg. If your systolic BP is consistently above 130-140 mmHg, reduce sodium intake, avoid stress, perform light cardio, and ensure your prescribed antihypertensives are taken on schedule."
                lower.contains("sugar") || lower.contains("glucose") || lower.contains("diabetes") ->
                    "Target fasting blood sugar is generally 70-100 mg/dL, and post-meal (2 hours) below 140 mg/dL. Keep meals rich in fiber, stay hydrated, and monitor blood glucose regularly."
                lower.contains("headache") || lower.contains("fever") || lower.contains("pain") ->
                    "Hydrate well and rest in a dim, quiet room. If a fever exceeds 101°F (38.3°C) or is accompanied by severe neck stiffness or rash, consult a doctor immediately."
                lower.contains("bmi") || lower.contains("weight") ->
                    "A healthy Body Mass Index (BMI) is between 18.5 and 24.9. Maintaining optimal weight reduces cardiovascular risk and improves joint health."
                lower.contains("water") || lower.contains("hydration") ->
                    "Adults should aim for 2.5 to 3 Liters (about 8-10 glasses) of water daily. Increase intake during warm weather or exercise."
                else ->
                    "Hello! I am your Medicare AI Health Assistant. I am here to help guide you regarding your medications, vital signs, reminders, and healthy living habits. For acute symptoms, please consult a qualified physician."
            }
        }
    }
}
