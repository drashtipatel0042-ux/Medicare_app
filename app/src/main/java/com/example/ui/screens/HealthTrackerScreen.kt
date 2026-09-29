package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthVitals
import com.example.data.model.VitalType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTrackerScreen(
    vitals: List<HealthVitals>,
    waterLogs: List<HealthVitals>,
    isGujarati: Boolean,
    onAddBp: (Double, Double, Double, String) -> Unit,
    onAddSugar: (Double, String) -> Unit,
    onAddBmi: (Double, Double) -> Unit,
    onAddWater: (Double) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = if (isGujarati) {
        listOf("BP", "સુગર", "BMI & વજન", "પાણી (Water)")
    } else {
        listOf("Blood Pressure", "Blood Sugar", "BMI & Weight", "Hydration")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("health_tracker_screen")
    ) {
        // Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> BloodPressureView(
                vitals.filter { it.vitalType == VitalType.BLOOD_PRESSURE },
                isGujarati,
                onAddBp
            )
            1 -> BloodSugarView(
                vitals.filter { it.vitalType == VitalType.BLOOD_SUGAR },
                isGujarati,
                onAddSugar
            )
            2 -> BmiCalculatorView(
                vitals.filter { it.vitalType == VitalType.BMI },
                isGujarati,
                onAddBmi
            )
            3 -> HydrationView(
                waterLogs,
                isGujarati,
                onAddWater
            )
        }
    }
}

@Composable
private fun BloodPressureView(
    bpLogs: List<HealthVitals>,
    isGujarati: Boolean,
    onAddBp: (Double, Double, Double, String) -> Unit
) {
    var systolic by remember { mutableStateOf("120") }
    var diastolic by remember { mutableStateOf("80") }
    var pulse by remember { mutableStateOf("72") }
    var note by remember { mutableStateOf("Resting") }
    var showDialog by remember { mutableStateOf(false) }

    val latest = bpLogs.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card showing latest BP
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isGujarati) "છેલ્લું બ્લડ પ્રેશર રીડિંગ" else "Latest Blood Pressure",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (latest != null) "${latest.value1.toInt()}/${latest.value2.toInt()}" else "120/80",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 38.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "mmHg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pulse: ${latest?.value3?.toInt() ?: 72} bpm • ${latest?.contextNote ?: "Normal Range"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Add Reading Button
        item {
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_bp_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGujarati) "નવું BP માપ ઉમેરો" else "Log New BP Reading")
            }
        }

        // History
        item {
            Text(
                text = if (isGujarati) "ઇતિહાસ (Reading History)" else "Reading History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(bpLogs) { vital ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${vital.value1.toInt()}/${vital.value2.toInt()} mmHg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Pulse: ${vital.value3.toInt()} bpm • ${vital.contextNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(vital.timestamp))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isGujarati) "બ્લડ પ્રેશર લોગ કરો" else "Log Blood Pressure") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { systolic = it },
                        label = { Text("Systolic (mmHg, e.g. 120)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { diastolic = it },
                        label = { Text("Diastolic (mmHg, e.g. 80)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pulse,
                        onValueChange = { pulse = it },
                        label = { Text("Pulse (bpm, e.g. 72)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Condition / Note") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sys = systolic.toDoubleOrNull() ?: 120.0
                    val dia = diastolic.toDoubleOrNull() ?: 80.0
                    val pul = pulse.toDoubleOrNull() ?: 72.0
                    onAddBp(sys, dia, pul, note)
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BloodSugarView(
    sugarLogs: List<HealthVitals>,
    isGujarati: Boolean,
    onAddSugar: (Double, String) -> Unit
) {
    var glucoseValue by remember { mutableStateOf("95") }
    var contextOption by remember { mutableStateOf("Fasting") }
    var showDialog by remember { mutableStateOf(false) }

    val latest = sugarLogs.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isGujarati) "છેલ્લું બ્લડ સુગર રીડિંગ" else "Latest Blood Glucose",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (latest != null) "${latest.value1.toInt()}" else "95",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 38.sp
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "mg/dL",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = latest?.contextNote ?: "Fasting (Normal: 70 - 100 mg/dL)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }

        item {
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGujarati) "સુગર રીડિંગ ઉમેરો" else "Log Glucose Reading")
            }
        }

        item {
            Text(
                text = if (isGujarati) "ઇતિહાસ (History)" else "Glucose Logs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(sugarLogs) { vital ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${vital.value1.toInt()} mg/dL",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = vital.contextNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    val dateFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(vital.timestamp))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log Blood Glucose") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = glucoseValue,
                        onValueChange = { glucoseValue = it },
                        label = { Text("Glucose mg/dL") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contextOption,
                        onValueChange = { contextOption = it },
                        label = { Text("Context (Fasting, Post-meal, Bedtime)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val glu = glucoseValue.toDoubleOrNull() ?: 95.0
                    onAddSugar(glu, contextOption)
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BmiCalculatorView(
    bmiLogs: List<HealthVitals>,
    isGujarati: Boolean,
    onAddBmi: (Double, Double) -> Unit
) {
    var weightInput by remember { mutableStateOf("68.5") }
    var heightInput by remember { mutableStateOf("172") }

    val w = weightInput.toDoubleOrNull() ?: 68.5
    val h = heightInput.toDoubleOrNull() ?: 172.0
    val hM = h / 100.0
    val calculatedBmi = if (hM > 0) String.format(Locale.US, "%.1f", w / (hM * hM)).toDoubleOrNull() ?: 23.1 else 23.1

    val category = when {
        calculatedBmi < 18.5 -> "Underweight"
        calculatedBmi < 25.0 -> "Normal Weight (Healthy)"
        calculatedBmi < 30.0 -> "Overweight"
        else -> "Obesity"
    }

    val categoryColor = when {
        calculatedBmi < 18.5 -> Color(0xFF0277BD)
        calculatedBmi < 25.0 -> Color(0xFF2E7D32)
        calculatedBmi < 30.0 -> Color(0xFFE65100)
        else -> Color(0xFFBA1A1A)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isGujarati) "તમારું બોડી માસ ઇન્ડેક્સ (BMI)" else "Your Body Mass Index (BMI)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$calculatedBmi",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = categoryColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = categoryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = category,
                        color = categoryColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text(if (isGujarati) "વજન (Weight kg)" else "Weight (kg)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text(if (isGujarati) "ઊંચાઈ (Height cm)" else "Height (cm)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Button(
            onClick = { onAddBmi(w, h) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isGujarati) "BMI અને વજન સાચવો" else "Save BMI & Weight Record")
        }
    }
}

@Composable
private fun HydrationView(
    waterLogs: List<HealthVitals>,
    isGujarati: Boolean,
    onAddWater: (Double) -> Unit
) {
    val totalWaterMl = waterLogs.sumOf { it.value1 }.toInt()
    val goalMl = 2500
    val progress = (totalWaterMl.toFloat() / goalMl).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF0277BD),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$totalWaterMl / $goalMl ml",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF01579B)
                    )
                )
                Text(
                    text = "${(progress * 100).toInt()}% of Daily Hydration Goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0277BD)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF0288D1),
                    trackColor = Color(0xFFB3E5FC)
                )
            }
        }

        // Quick add buttons
        Text(
            text = if (isGujarati) "ઝડપી પાણી ઉમેરો:" else "Quick Add Water Intake:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onAddWater(250.0) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+250 ml (1 Glass)")
            }
            Button(
                onClick = { onAddWater(500.0) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+500 ml (Bottle)")
            }
        }
    }
}
