package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.viewmodel.Screen

@Composable
fun DashboardScreen(
    patientProfile: PatientProfile?,
    medicines: List<Medicine>,
    todayLogs: List<DoseLog>,
    lowStockMedicines: List<Medicine>,
    vitals: List<HealthVitals>,
    waterLogs: List<HealthVitals>,
    isGujarati: Boolean,
    hasExactAlarmPermission: Boolean = true,
    onRequestExactAlarmPermission: () -> Unit = {},
    onTriggerTestAlarm: () -> Unit = {},
    onNavigate: (Screen) -> Unit,
    onTakeDose: (DoseLog) -> Unit,
    onRefillMedicine: (Long) -> Unit,
    onSelectMedicine: (Medicine) -> Unit
) {
    val totalDoses = todayLogs.size
    val takenDoses = todayLogs.count { it.status == DoseStatus.TAKEN }
    val adherencePercent = if (totalDoses > 0) ((takenDoses.toFloat() / totalDoses) * 100).toInt() else 100
    val totalWaterMl = waterLogs.sumOf { it.value1 }.toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning Banner if exact alarm permission is missing
        if (!hasExactAlarmPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AlarmOff, contentDescription = null, tint = Color(0xFFC62828))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGujarati) "ચોક્કસ એલાર્મ પરવાનગી જરૂરી છે" else "Exact Alarm Permission Required",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isGujarati) "ફોન સ્લીપ/બંધ હોય ત્યારે દવા રિમાઇન્ડર સમયસર વાગે તે માટે પરવાનગી આપો." else "To ensure reminders ring on time when the device is locked or the app is closed, please enable exact alarms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestExactAlarmPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isGujarati) "પરવાનગી સક્ષમ કરો" else "Enable Exact Alarms in Settings")
                        }
                    }
                }
            }
        }
        // Patient Greeting Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Patient Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGujarati) "સુપ્રભાત," else "Good day,",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = patientProfile?.fullName?.takeIf { it.isNotBlank() } ?: "Patient",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Blood Group: ${patientProfile?.bloodGroup ?: "B+"}  •  ${patientProfile?.age ?: 42} Yrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                    IconButton(
                        onClick = { onNavigate(Screen.PatientQrId) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .testTag("qr_card_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Medical QR ID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Today's Adherence & Stats Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isGujarati) "આજનું દવા પાલન (Adherence)" else "Today's Medicine Adherence",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$takenDoses / $totalDoses ${if (isGujarati) "દવા લીધી" else "doses taken"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "$adherencePercent%",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (adherencePercent >= 80) Color(0xFF006A6A) else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { if (totalDoses > 0) takenDoses.toFloat() / totalDoses else 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (adherencePercent >= 80) Color(0xFF006A6A) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Alarm Engine & Exact Alarm Test Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGujarati) "ચોક્કસ એલાર્મ પરીક્ષણ" else "Exact Alarm Engine",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = if (isGujarati) "ઍપ બંધ હોય ત્યારે પણ અવાજ અને વાઇબ્રેશન સાથે વાગશે" else "Rings with sound & vibration even when app is closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(
                        onClick = onTriggerTestAlarm,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("test_alarm_button")
                    ) {
                        Text(text = if (isGujarati) "ટેસ્ટ એલાર્મ (૫ સે)" else "Test Alarm (5s)", fontSize = 12.sp)
                    }
                }
            }
        }

        // Low Stock Alert Banner (if any)
        if (lowStockMedicines.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low Stock",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGujarati) "સ્ટોક રિફિલ ચેતવણી (${lowStockMedicines.size})" else "Low Stock Refill Alert (${lowStockMedicines.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        lowStockMedicines.forEach { med ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${med.name} (${med.remainingStock} left)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4E342E)
                                )
                                Button(
                                    onClick = { onRefillMedicine(med.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                ) {
                                    Text(text = if (isGujarati) "રીફિલ કરો" else "Refill", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Grid (Add Med, SOS, AI Chat, Appointments, Vitals, PDF)
        item {
            Text(
                text = if (isGujarati) "ઝડપી સેવાઓ (Quick Actions)" else "Quick Health Services",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = if (isGujarati) "દવા ઉમેરો" else "Add Med",
                    icon = Icons.Default.AddCircle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AddMedicine) }
                )
                QuickActionCard(
                    title = if (isGujarati) "ઇમરજન્સી SOS" else "SOS Alert",
                    icon = Icons.Default.Sos,
                    color = Color(0xFFBA1A1A),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.EmergencySos) }
                )
                QuickActionCard(
                    title = if (isGujarati) "AI સહાયક" else "AI Chat",
                    icon = Icons.Default.SmartToy,
                    color = Color(0xFF006A6A),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AiChat) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = if (isGujarati) "ડૉક્ટર એપોઇન્ટમેન્ટ" else "Doctor Visit",
                    icon = Icons.Default.CalendarMonth,
                    color = Color(0xFF5A489B),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Appointments) }
                )
                QuickActionCard(
                    title = if (isGujarati) "પ્રિસ્ક્રિપ્શન" else "Prescription",
                    icon = Icons.Default.Description,
                    color = Color(0xFF005AC1),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Prescriptions) }
                )
                QuickActionCard(
                    title = if (isGujarati) "PDF રિપોર્ટ" else "PDF Report",
                    icon = Icons.Default.FileDownload,
                    color = Color(0xFF2E6B38),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.ExportReport) }
                )
            }
        }

        // Today's Upcoming Doses Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGujarati) "આજની દવાઓનું શિડ્યુલ" else "Today's Medicine Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onNavigate(Screen.TodayMedicines) }) {
                    Text(text = if (isGujarati) "બધી જુઓ" else "View All")
                }
            }
        }

        if (todayLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isGujarati) "આજના માટે કોઈ દવા શિડ્યુલ નથી." else "No medicine scheduled for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(todayLogs.take(4)) { log ->
                val med = medicines.find { it.id == log.medicineId }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (log.status == DoseStatus.TAKEN) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { med?.let { onSelectMedicine(it) } }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (log.status == DoseStatus.TAKEN) Color(0xFFD7F8E8) else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (log.status == DoseStatus.TAKEN) Icons.Default.Check else Icons.Default.Medication,
                                contentDescription = null,
                                tint = if (log.status == DoseStatus.TAKEN) Color(0xFF006A6A) else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.medicineName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${log.dosage} • ${log.scheduledTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        if (log.status == DoseStatus.TAKEN) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = if (isGujarati) "લીધી ✓" else "Taken ✓",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onTakeDose(log) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("take_dose_btn_${log.id}")
                            ) {
                                Text(text = if (isGujarati) "લેવી" else "Take")
                            }
                        }
                    }
                }
            }
        }

        // Quick Vitals Snapshot
        item {
            Text(
                text = if (isGujarati) "આરોગ્ય માપદંડ (Vitals Glance)" else "Health Vitals Glance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Blood Pressure Card
                val bp = vitals.firstOrNull { it.vitalType == VitalType.BLOOD_PRESSURE }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.HealthTracker) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFBA1A1A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "BP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bp != null) "${bp.value1.toInt()}/${bp.value2.toInt()}" else "120/80",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(text = "mmHg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Water Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.HealthTracker) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF0277BD),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Water", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalWaterMl ml",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(text = "Goal: 2500 ml", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // BMI Card
                val bmi = vitals.firstOrNull { it.vitalType == VitalType.BMI }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.HealthTracker) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "BMI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bmi != null) "${bmi.value1}" else "23.1",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(text = "Normal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}
