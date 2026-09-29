package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DoseStatus
import com.example.ui.components.MedicareBottomBar
import com.example.ui.components.MedicareTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MedicareTheme
import com.example.ui.viewmodel.MedicareViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MedicareViewModel = viewModel()

            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val isGujarati by viewModel.isGujarati.collectAsStateWithLifecycle()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val voiceRemindersEnabled by viewModel.voiceRemindersEnabled.collectAsStateWithLifecycle()

            val medicines by viewModel.allMedicines.collectAsStateWithLifecycle()
            val lowStockMeds by viewModel.lowStockMedicines.collectAsStateWithLifecycle()
            val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
            val vitals by viewModel.allVitals.collectAsStateWithLifecycle()
            val waterLogs by viewModel.waterLogs.collectAsStateWithLifecycle()
            val prescriptions by viewModel.prescriptions.collectAsStateWithLifecycle()
            val appointments by viewModel.appointments.collectAsStateWithLifecycle()
            val contacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
            val profile by viewModel.patientProfile.collectAsStateWithLifecycle()
            val selectedMedicine by viewModel.selectedMedicine.collectAsStateWithLifecycle()

            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
            val isSirenActive by viewModel.isSirenActive.collectAsStateWithLifecycle()
            val generatedPdf by viewModel.generatedPdfFile.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            var hasExactAlarmPermission by remember {
                mutableStateOf(viewModel.canScheduleExactAlarms())
            }

            // Request Notification Permission on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Please allow notifications for medicine alarms.")
                    }
                }
            }

            val openPhoneLogin = {
                val loginIntent = android.content.Intent(this@MainActivity, com.example.auth.PhoneLoginActivity::class.java)
                startActivity(loginIntent)
            }

            // Check permissions on start and refresh
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                hasExactAlarmPermission = viewModel.canScheduleExactAlarms()

                // Check if user is authenticated with Firebase Phone Auth
                val isGuest = intent?.getBooleanExtra("is_guest", false) ?: false
                if (!com.example.auth.UserSessionManager.isLoggedIn(this@MainActivity) && !isGuest) {
                    openPhoneLogin()
                }

                // Handle intent navigation from notification clicks
                val navigateTo = intent?.getStringExtra("navigate_to")
                if (navigateTo == "today_medicines") {
                    viewModel.navigateTo(Screen.TodayMedicines)
                }
            }

            // Back handler on secondary screens
            if (currentScreen != Screen.Dashboard && currentScreen != Screen.Splash && currentScreen != Screen.Auth) {
                BackHandler {
                    viewModel.navigateTo(Screen.Dashboard)
                }
            }

            MedicareTheme(darkTheme = isDarkMode) {
                val showBottomNav = currentScreen in listOf(
                    Screen.Dashboard,
                    Screen.TodayMedicines,
                    Screen.HealthTracker,
                    Screen.AiChat,
                    Screen.EmergencySos
                )

                val showTopBar = currentScreen !in listOf(Screen.Splash, Screen.Auth)
                val isSubScreen = currentScreen !in listOf(
                    Screen.Dashboard,
                    Screen.TodayMedicines,
                    Screen.HealthTracker,
                    Screen.AiChat,
                    Screen.EmergencySos
                )

                val topBarTitle = when (currentScreen) {
                    Screen.Dashboard -> if (isGujarati) "મેડિકેર હેલ્થ" else "Medicare"
                    Screen.TodayMedicines -> if (isGujarati) "આજની દવાઓ" else "Today's Schedule"
                    Screen.HealthTracker -> if (isGujarati) "હેલ્થ ટ્રેકર" else "Health Tracker"
                    Screen.AiChat -> if (isGujarati) "AI હેલ્થ આસિસ્ટન્ટ" else "Medicare AI Chat"
                    Screen.EmergencySos -> if (isGujarati) "ઇમરજન્સી SOS" else "Emergency SOS"
                    Screen.AddMedicine -> if (isGujarati) "દવા ઉમેરો" else "Add Medicine"
                    Screen.MedicineDetail -> selectedMedicine?.name ?: "Medicine"
                    Screen.Appointments -> if (isGujarati) "એપોઇન્ટમેન્ટ્સ" else "Doctor Visits"
                    Screen.Prescriptions -> if (isGujarati) "પ્રિસ્ક્રિપ્શન" else "Prescriptions"
                    Screen.PatientQrId -> if (isGujarati) "મેડિકલ QR ID" else "Emergency Medical ID"
                    Screen.ExportReport -> if (isGujarati) "PDF રિપોર્ટ" else "Export Clinical Report"
                    Screen.AdminDashboard -> if (isGujarati) "એડમિન પોર્ટલ" else "Admin & Clinician Portal"
                    Screen.Settings -> if (isGujarati) "સેટિંગ્સ" else "Settings"
                    else -> "Medicare"
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (showTopBar) {
                            MedicareTopBar(
                                title = topBarTitle,
                                isGujarati = isGujarati,
                                userName = profile?.fullName ?: com.example.auth.UserSessionManager.getUserName(this@MainActivity),
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onSosClick = { viewModel.navigateTo(Screen.EmergencySos) },
                                onAdminClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                                onSettingsClick = { viewModel.navigateTo(Screen.Settings) },
                                onProfileClick = { openPhoneLogin() },
                                showBackButton = isSubScreen,
                                onBackClick = { viewModel.navigateTo(Screen.Dashboard) }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomNav) {
                            MedicareBottomBar(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.navigateTo(it) },
                                isGujarati = isGujarati
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Splash -> {
                                SplashScreen(
                                    isGujarati = isGujarati,
                                    onGetStarted = { viewModel.navigateTo(Screen.Dashboard) }
                                )
                            }
                            Screen.Auth -> {
                                AuthScreen(
                                    isGujarati = isGujarati,
                                    onAuthSuccess = { viewModel.navigateTo(Screen.Dashboard) },
                                    onOpenPhoneAuth = { openPhoneLogin() }
                                )
                            }
                            Screen.Dashboard -> {
                                DashboardScreen(
                                    patientProfile = profile,
                                    medicines = medicines,
                                    todayLogs = todayLogs,
                                    lowStockMedicines = lowStockMeds,
                                    vitals = vitals,
                                    waterLogs = waterLogs,
                                    isGujarati = isGujarati,
                                    hasExactAlarmPermission = hasExactAlarmPermission,
                                    onRequestExactAlarmPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                android.net.Uri.parse("package:$packageName")
                                            )
                                            startActivity(intent)
                                        }
                                    },
                                    onTriggerTestAlarm = {
                                        viewModel.triggerTestAlarm(5)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isGujarati) "૫ સેકન્ડમાં એલાર્મ વાગશે! ઍપ મિનિમાઇઝ કરીને ચેક કરો."
                                                else "🔔 Alarm scheduled in 5 seconds! Put app in background or lock screen to test."
                                            )
                                        }
                                    },
                                    onNavigate = { viewModel.navigateTo(it) },
                                    onTakeDose = { log ->
                                        viewModel.markDoseStatus(log.id, DoseStatus.TAKEN, log.medicineId, log.medicineName)
                                    },
                                    onRefillMedicine = { medId -> viewModel.refillMedicine(medId) },
                                    onSelectMedicine = { med ->
                                        viewModel.setSelectedMedicine(med)
                                        viewModel.navigateTo(Screen.MedicineDetail)
                                    }
                                )
                            }
                            Screen.TodayMedicines -> {
                                TodayMedicinesScreen(
                                    todayLogs = todayLogs,
                                    medicines = medicines,
                                    isGujarati = isGujarati,
                                    onTakeDose = { log ->
                                        viewModel.markDoseStatus(log.id, DoseStatus.TAKEN, log.medicineId, log.medicineName)
                                    },
                                    onSnoozeDose = { log ->
                                        viewModel.markDoseStatus(log.id, DoseStatus.SNOOZED, log.medicineId, log.medicineName)
                                    },
                                    onSpeakReminder = { name, time ->
                                        viewModel.speakReminder(name, time)
                                    },
                                    onSelectMedicine = { med ->
                                        viewModel.setSelectedMedicine(med)
                                        viewModel.navigateTo(Screen.MedicineDetail)
                                    },
                                    onNavigateToAddMedicine = {
                                        viewModel.setSelectedMedicine(null)
                                        viewModel.navigateTo(Screen.AddMedicine)
                                    }
                                )
                            }
                            Screen.AddMedicine -> {
                                AddEditMedicineScreen(
                                    medicineToEdit = selectedMedicine,
                                    isGujarati = isGujarati,
                                    onSave = { med ->
                                        if (selectedMedicine != null) {
                                            viewModel.updateMedicine(med) {
                                                viewModel.navigateTo(Screen.Dashboard)
                                            }
                                        } else {
                                            viewModel.addMedicine(med) {
                                                viewModel.navigateTo(Screen.Dashboard)
                                            }
                                        }
                                    },
                                    onCancel = { viewModel.navigateTo(Screen.Dashboard) }
                                )
                            }
                            Screen.MedicineDetail -> {
                                MedicineDetailScreen(
                                    medicine = selectedMedicine,
                                    isGujarati = isGujarati,
                                    onEdit = { med ->
                                        viewModel.setSelectedMedicine(med)
                                        viewModel.navigateTo(Screen.AddMedicine)
                                    },
                                    onDelete = { med ->
                                        viewModel.deleteMedicine(med) {
                                            viewModel.navigateTo(Screen.Dashboard)
                                        }
                                    },
                                    onRefill = { medId -> viewModel.refillMedicine(medId) },
                                    onBack = { viewModel.navigateTo(Screen.Dashboard) }
                                )
                            }
                            Screen.HealthTracker -> {
                                HealthTrackerScreen(
                                    vitals = vitals,
                                    waterLogs = waterLogs,
                                    isGujarati = isGujarati,
                                    onAddBp = { sys, dia, pulse, note -> viewModel.addBloodPressure(sys, dia, pulse, note) },
                                    onAddSugar = { glu, note -> viewModel.addBloodSugar(glu, note) },
                                    onAddBmi = { weight, height -> viewModel.addBmi(weight, height) },
                                    onAddWater = { amt -> viewModel.addWaterIntake(amt) }
                                )
                            }
                            Screen.EmergencySos -> {
                                EmergencySosScreen(
                                    contacts = contacts,
                                    profile = profile,
                                    isSirenActive = isSirenActive,
                                    isGujarati = isGujarati,
                                    onToggleSiren = { viewModel.toggleSosSiren() },
                                    onDialContact = { phone -> viewModel.dialEmergency(phone) },
                                    onSendSosSms = { contact -> viewModel.sendSosSms(contact) },
                                    onAddContact = { name, rel, phone, isPrimary ->
                                        viewModel.addEmergencyContact(name, rel, phone, isPrimary)
                                    }
                                )
                            }
                            Screen.Appointments -> {
                                AppointmentsScreen(
                                    appointments = appointments,
                                    isGujarati = isGujarati,
                                    onBookAppointment = { apt -> viewModel.bookAppointment(apt) }
                                )
                            }
                            Screen.Prescriptions -> {
                                PrescriptionsScreen(
                                    prescriptions = prescriptions,
                                    isGujarati = isGujarati,
                                    onAddPrescription = { rx -> viewModel.addPrescription(rx) }
                                )
                            }
                            Screen.AiChat -> {
                                AiChatScreen(
                                    messages = chatMessages,
                                    isLoading = isChatLoading,
                                    isGujarati = isGujarati,
                                    onSendMessage = { text -> viewModel.sendChatMessage(text) },
                                    onSpeakText = { text ->
                                        viewModel.speakReminder(text, "")
                                    }
                                )
                            }
                            Screen.PatientQrId -> {
                                PatientQrIdScreen(
                                    profile = profile,
                                    isGujarati = isGujarati
                                )
                            }
                            Screen.ExportReport -> {
                                ExportReportScreen(
                                    generatedFile = generatedPdf,
                                    isGujarati = isGujarati,
                                    onGeneratePdf = { viewModel.exportReportPdf() },
                                    onSharePdf = { viewModel.sharePdfReport() }
                                )
                            }
                            Screen.AdminDashboard -> {
                                AdminDashboardScreen(
                                    medicines = medicines,
                                    isGujarati = isGujarati
                                )
                            }
                            Screen.Settings -> {
                                SettingsScreen(
                                    isGujarati = isGujarati,
                                    isDarkMode = isDarkMode,
                                    voiceRemindersEnabled = voiceRemindersEnabled,
                                    patientProfile = profile,
                                    onToggleLanguage = { viewModel.toggleLanguage() },
                                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                                    onToggleVoiceReminders = { viewModel.toggleVoiceReminders() },
                                    onOpenPhoneLogin = { openPhoneLogin() },
                                    onLogout = {
                                        com.example.auth.UserSessionManager.logout(this@MainActivity)
                                        openPhoneLogin()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
