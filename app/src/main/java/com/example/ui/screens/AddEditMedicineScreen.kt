package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    medicineToEdit: Medicine?,
    isGujarati: Boolean,
    onSave: (Medicine) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(medicineToEdit?.name ?: "") }
    var dosage by remember { mutableStateOf(medicineToEdit?.dosage ?: "500 mg") }
    var selectedType by remember { mutableStateOf(medicineToEdit?.type ?: MedicineType.TABLET) }
    var selectedTiming by remember { mutableStateOf(medicineToEdit?.foodTiming ?: FoodTiming.AFTER_FOOD) }
    var selectedSchedule by remember { mutableStateOf(medicineToEdit?.repeatSchedule ?: RepeatSchedule.DAILY) }

    var selectedTimes by remember {
        mutableStateOf(medicineToEdit?.reminderTimes?.toMutableList() ?: mutableListOf("08:00 AM", "08:00 PM"))
    }
    var customTimeInput by remember { mutableStateOf("") }
    var showCustomTimeDialog by remember { mutableStateOf(false) }

    var totalStock by remember { mutableStateOf((medicineToEdit?.totalStock ?: 30).toString()) }
    var refillThreshold by remember { mutableStateOf((medicineToEdit?.refillThreshold ?: 5).toString()) }
    var notes by remember { mutableStateOf(medicineToEdit?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }

    val presetTimes = listOf("08:00 AM", "01:00 PM", "05:00 PM", "08:30 PM", "10:00 PM")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("add_edit_medicine_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (medicineToEdit != null) {
                if (isGujarati) "દવા સુધારો (Edit Medicine)" else "Edit Medicine"
            } else {
                if (isGujarati) "નવી દવા ઉમેરો (Add Medicine)" else "Add New Medicine"
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        // Medicine Name
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = it.isBlank()
            },
            label = { Text(if (isGujarati) "દવાનું નામ *" else "Medicine Name *") },
            placeholder = { Text("e.g. Paracetamol, Metformin") },
            isError = nameError,
            supportingText = { if (nameError) Text("Name cannot be empty") },
            leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("med_name_input")
        )

        // Dosage
        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text(if (isGujarati) "ડોઝ (Dosage)" else "Dosage") },
            placeholder = { Text("e.g. 500 mg, 1 tablet, 10 ml") },
            leadingIcon = { Icon(Icons.Default.Science, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("med_dosage_input")
        )

        // Medicine Type Chips
        Text(
            text = if (isGujarati) "દવાનો પ્રકાર (Medicine Type)" else "Medicine Type",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MedicineType.values()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(if (isGujarati) type.labelGu else type.labelEn) }
                )
            }
        }

        // Food Timing Chips
        Text(
            text = if (isGujarati) "ખોરાકનો સમય (Food Timing)" else "Food Timing",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FoodTiming.values()) { timing ->
                FilterChip(
                    selected = selectedTiming == timing,
                    onClick = { selectedTiming = timing },
                    label = { Text(if (isGujarati) timing.labelGu else timing.labelEn) }
                )
            }
        }

        // Multiple Reminder Times
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGujarati) "રિમાઇન્ડર સમય (Reminder Times)" else "Reminder Schedule (${selectedTimes.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showCustomTimeDialog = true }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isGujarati) "સમય ઉમેરો" else "+ Custom")
                    }
                }

                Text(
                    text = if (isGujarati) "ઝડપી પસંદગી (Quick presets):" else "Quick preset times:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetTimes) { time ->
                        val isSelected = selectedTimes.contains(time)
                        SuggestionChip(
                            onClick = {
                                if (isSelected) {
                                    if (selectedTimes.size > 1) selectedTimes.remove(time)
                                } else {
                                    selectedTimes.add(time)
                                }
                            },
                            label = { Text(time) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isGujarati) "સક્રિય રિમાઇન્ડર્સ:" else "Active Reminders:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedTimes.forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = {
                                if (selectedTimes.size > 1) {
                                    selectedTimes = selectedTimes.toMutableList().also { it.remove(time) }
                                }
                            },
                            label = { Text(time) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }

        // Stock & Refill Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = totalStock,
                onValueChange = { totalStock = it.filter { ch -> ch.isDigit() } },
                label = { Text(if (isGujarati) "કુલ સ્ટોક" else "Total Stock") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = refillThreshold,
                onValueChange = { refillThreshold = it.filter { ch -> ch.isDigit() } },
                label = { Text(if (isGujarati) "રિફિલ એલર્ટ" else "Refill Alert At") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Notes / Guidelines
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(if (isGujarati) "ડૉક્ટરની નોંધ / સૂચના" else "Doctor's Notes & Advice") },
            placeholder = { Text("e.g. Take with warm water, avoid dairy") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isGujarati) "રદ કરો" else "Cancel")
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val stock = totalStock.toIntOrNull() ?: 30
                    val threshold = refillThreshold.toIntOrNull() ?: 5
                    val times = if (selectedTimes.isEmpty()) listOf("08:00 AM") else selectedTimes.toList()

                    val newMed = Medicine(
                        id = medicineToEdit?.id ?: 0L,
                        name = name.trim(),
                        dosage = dosage.trim(),
                        type = selectedType,
                        foodTiming = selectedTiming,
                        reminderTimes = times,
                        startDate = medicineToEdit?.startDate ?: todayStr,
                        repeatSchedule = selectedSchedule,
                        totalStock = stock,
                        remainingStock = medicineToEdit?.remainingStock ?: stock,
                        refillThreshold = threshold,
                        notes = notes.trim(),
                        isActive = true
                    )
                    onSave(newMed)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_medicine_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isGujarati) "સાચવો" else "Save Medicine")
            }
        }
    }

    // Custom Time Dialog
    if (showCustomTimeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimeDialog = false },
            title = { Text(if (isGujarati) "કસ્ટમ સમય દાખલ કરો" else "Add Custom Time") },
            text = {
                OutlinedTextField(
                    value = customTimeInput,
                    onValueChange = { customTimeInput = it },
                    label = { Text("Time (e.g. 02:30 PM)") },
                    placeholder = { Text("02:30 PM") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (customTimeInput.isNotBlank()) {
                        selectedTimes = selectedTimes.toMutableList().also { it.add(customTimeInput.trim()) }
                        customTimeInput = ""
                    }
                    showCustomTimeDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
