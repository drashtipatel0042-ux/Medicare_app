package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedicareApp
import com.example.alarm.AlarmScheduler
import com.example.data.model.*
import com.example.service.EmergencySosManager
import com.example.service.GeminiHealthService
import com.example.service.PdfExportService
import com.example.service.TtsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object TodayMedicines : Screen("today_medicines")
    object AddMedicine : Screen("add_medicine")
    object MedicineDetail : Screen("medicine_detail")
    object HealthTracker : Screen("health_tracker")
    object EmergencySos : Screen("emergency_sos")
    object Appointments : Screen("appointments")
    object Prescriptions : Screen("prescriptions")
    object AiChat : Screen("ai_chat")
    object PatientQrId : Screen("patient_qr_id")
    object ExportReport : Screen("export_report")
    object AdminDashboard : Screen("admin_dashboard")
    object Settings : Screen("settings")
}

class MedicareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MedicareApp).repository
    private val ttsManager = TtsManager(application)
    private val sosManager = EmergencySosManager(application)
    private val pdfService = PdfExportService(application)
    private val geminiService = GeminiHealthService()

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // App Preferences
    private val _isGujarati = MutableStateFlow(false)
    val isGujarati: StateFlow<Boolean> = _isGujarati.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _voiceRemindersEnabled = MutableStateFlow(true)
    val voiceRemindersEnabled: StateFlow<Boolean> = _voiceRemindersEnabled.asStateFlow()

    // Selected medicine for details
    private val _selectedMedicine = MutableStateFlow<Medicine?>(null)
    val selectedMedicine: StateFlow<Medicine?> = _selectedMedicine.asStateFlow()

    // Medicines Flow
    val allMedicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockMedicines: StateFlow<List<Medicine>> = repository.lowStockMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dose Logs Flow
    private val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayLogs: StateFlow<List<DoseLog>> = repository.getLogsForDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missedLogs: StateFlow<List<DoseLog>> = repository.missedLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vitals Flow
    val allVitals: StateFlow<List<HealthVitals>> = repository.allVitals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLogs: StateFlow<List<HealthVitals>> = repository.getWaterLogsForDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Prescriptions, Appointments, Contacts, Profile
    val prescriptions: StateFlow<List<Prescription>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<DoctorAppointment>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patientProfile: StateFlow<PatientProfile?> = repository.patientProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your Medicare AI Assistant. How can I help you with your health, medicines, or symptoms today?",
                isFromUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Emergency Siren state
    private val _isSirenActive = MutableStateFlow(false)
    val isSirenActive: StateFlow<Boolean> = _isSirenActive.asStateFlow()

    // PDF Export state
    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    // Search and filter for medicines
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<MedicineType?>(null)

    val filteredMedicines: StateFlow<List<Medicine>> = combine(
        allMedicines,
        searchQuery,
        filterType
    ) { meds, query, type ->
        meds.filter { med ->
            val matchesQuery = query.isBlank() || med.name.contains(query, ignoreCase = true) || med.dosage.contains(query, ignoreCase = true)
            val matchesType = type == null || med.type == type
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation actions
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSelectedMedicine(medicine: Medicine?) {
        _selectedMedicine.value = medicine
    }

    fun toggleLanguage() {
        _isGujarati.value = !_isGujarati.value
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleVoiceReminders() {
        _voiceRemindersEnabled.value = !_voiceRemindersEnabled.value
    }

    // Medicine actions
    fun addMedicine(medicine: Medicine, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveMedicine(medicine)
            // also create a pending log for today
            medicine.reminderTimes.forEach { time ->
                repository.addDoseLog(
                    DoseLog(
                        medicineId = medicine.id,
                        medicineName = medicine.name,
                        dosage = medicine.dosage,
                        scheduledTime = time,
                        date = todayDateString,
                        status = DoseStatus.PENDING
                    )
                )
            }
            onComplete()
        }
    }

    fun updateMedicine(medicine: Medicine, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateMedicine(medicine)
            _selectedMedicine.value = medicine
            onComplete()
        }
    }

    fun deleteMedicine(medicine: Medicine, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            if (_selectedMedicine.value?.id == medicine.id) {
                _selectedMedicine.value = null
            }
            onComplete()
        }
    }

    fun triggerTestAlarm(delaySeconds: Int = 5) {
        AlarmScheduler.scheduleTestAlarm(getApplication<Application>(), delaySeconds)
    }

    fun canScheduleExactAlarms(): Boolean {
        return AlarmScheduler.canScheduleExactAlarms(getApplication<Application>())
    }

    fun refillMedicine(medicineId: Long) {
        viewModelScope.launch {
            repository.refillMedicineStock(medicineId)
            val updated = repository.getMedicineById(medicineId)
            _selectedMedicine.value = updated
        }
    }

    // Dose actions
    fun markDoseStatus(logId: Long, status: DoseStatus, medicineId: Long, medicineName: String) {
        viewModelScope.launch {
            repository.recordDoseAction(logId, status, medicineId)
            if (_voiceRemindersEnabled.value) {
                if (status == DoseStatus.TAKEN) {
                    val msg = if (_isGujarati.value) "$medicineName દવા લેવાઈ ગઈ. ખૂબ સરસ!" else "$medicineName marked as taken. Great job!"
                    ttsManager.speak(msg, _isGujarati.value)
                }
            }
        }
    }

    fun speakReminder(medicineName: String, time: String) {
        val msg = if (_isGujarati.value) {
            "ધ્યાન આપો, $time વાગ્યે $medicineName દવા લેવાનો સમય થઈ ગયો છે."
        } else {
            "Attention, it is time to take your $medicineName scheduled for $time."
        }
        ttsManager.speak(msg, _isGujarati.value)
    }

    // Vitals actions
    fun addBloodPressure(systolic: Double, diastolic: Double, pulse: Double, note: String) {
        viewModelScope.launch {
            repository.recordVital(
                HealthVitals(
                    vitalType = VitalType.BLOOD_PRESSURE,
                    value1 = systolic,
                    value2 = diastolic,
                    value3 = pulse,
                    contextNote = note,
                    dateString = todayDateString
                )
            )
        }
    }

    fun addBloodSugar(glucoseMgDl: Double, contextNote: String) {
        viewModelScope.launch {
            repository.recordVital(
                HealthVitals(
                    vitalType = VitalType.BLOOD_SUGAR,
                    value1 = glucoseMgDl,
                    contextNote = contextNote,
                    dateString = todayDateString
                )
            )
        }
    }

    fun addBmi(weightKg: Double, heightCm: Double) {
        val heightM = heightCm / 100.0
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 0.0
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal weight"
            bmi < 30.0 -> "Overweight"
            else -> "Obesity"
        }
        viewModelScope.launch {
            repository.recordVital(
                HealthVitals(
                    vitalType = VitalType.BMI,
                    value1 = Math.round(bmi * 10.0) / 10.0,
                    value2 = heightCm,
                    value3 = weightKg,
                    contextNote = category,
                    dateString = todayDateString
                )
            )
            // also update patient profile
            patientProfile.value?.let { currentProfile ->
                repository.savePatientProfile(currentProfile.copy(heightCm = heightCm, weightKg = weightKg))
            }
        }
    }

    fun addWaterIntake(amountMl: Double = 250.0) {
        viewModelScope.launch {
            repository.recordVital(
                HealthVitals(
                    vitalType = VitalType.WATER,
                    value1 = amountMl,
                    contextNote = "Hydration glass",
                    dateString = todayDateString
                )
            )
        }
    }

    // Prescriptions & Appointments
    fun addPrescription(prescription: Prescription) {
        viewModelScope.launch {
            repository.savePrescription(prescription)
        }
    }

    fun bookAppointment(appointment: DoctorAppointment) {
        viewModelScope.launch {
            repository.bookAppointment(appointment)
        }
    }

    // Emergency SOS
    fun toggleSosSiren() {
        if (_isSirenActive.value) {
            sosManager.stopSirenAndAlert()
            _isSirenActive.value = false
        } else {
            sosManager.startSirenAndAlert()
            _isSirenActive.value = true
        }
    }

    fun stopSos() {
        sosManager.stopSirenAndAlert()
        _isSirenActive.value = false
    }

    fun dialEmergency(phone: String) {
        sosManager.dialEmergencyContact(phone)
    }

    fun sendSosSms(contact: EmergencyContact) {
        val patientName = patientProfile.value?.fullName ?: "Patient"
        val loc = patientProfile.value?.address ?: "Current location coordinates"
        sosManager.sendSosSms(contact.phoneNumber, patientName, loc)
    }

    fun addEmergencyContact(name: String, relationship: String, phone: String, isPrimary: Boolean) {
        viewModelScope.launch {
            repository.saveContact(
                EmergencyContact(
                    name = name,
                    relationship = relationship,
                    phoneNumber = phone,
                    isPrimarySos = isPrimary
                )
            )
        }
    }

    fun updateProfile(profile: PatientProfile) {
        viewModelScope.launch {
            repository.savePatientProfile(profile)
        }
    }

    // Chatbot
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(text = userText, isFromUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val responseText = geminiService.getHealthAdvice(userText, _isGujarati.value)
            val aiMsg = ChatMessage(text = responseText, isFromUser = false)
            _chatMessages.value = _chatMessages.value + aiMsg
            _isChatLoading.value = false
        }
    }

    // PDF Export
    fun exportReportPdf() {
        viewModelScope.launch {
            val profile = patientProfile.value ?: PatientProfile()
            val meds = allMedicines.value
            val vits = allVitals.value
            val file = pdfService.generateHealthReport(profile, meds, vits)
            _generatedPdfFile.value = file
        }
    }

    fun sharePdfReport() {
        _generatedPdfFile.value?.let { file ->
            pdfService.shareReport(file)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        sosManager.stopSirenAndAlert()
    }
}
