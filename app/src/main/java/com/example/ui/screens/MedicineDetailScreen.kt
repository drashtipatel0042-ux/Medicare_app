package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.Medicine
import com.example.data.model.RepeatSchedule

@Composable
fun MedicineDetailScreen(
    medicine: Medicine?,
    isGujarati: Boolean,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onRefill: (Long) -> Unit,
    onBack: () -> Unit
) {
    if (medicine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Medicine not found")
        }
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val stockPercent = if (medicine.totalStock > 0) medicine.remainingStock.toFloat() / medicine.totalStock else 0f
    val isLowStock = medicine.remainingStock <= medicine.refillThreshold

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("medicine_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Hero Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${medicine.dosage} • ${medicine.type.labelEn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Stock & Refill Tracker
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
                            text = if (isGujarati) "સ્ટોક અને રિફિલ ટ્રેકર" else "Stock & Inventory",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${medicine.remainingStock} of ${medicine.totalStock} units remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLowStock) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(
                        onClick = { onRefill(medicine.id) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLowStock) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isGujarati) "રીફિલ કરો" else "Refill Stock")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { stockPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isLowStock) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                if (isLowStock) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Low stock warning: Remaining units are below threshold (${medicine.refillThreshold}).",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBA1A1A)
                    )
                }
            }
        }

        // Schedule and Timing Details
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isGujarati) "શિડ્યુલ અને નિયમો" else "Schedule & Guidelines",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                DetailRow(
                    label = if (isGujarati) "ખોરાકનો સમય" else "Food Timing",
                    value = if (isGujarati) medicine.foodTiming.labelGu else medicine.foodTiming.labelEn
                )
                DetailRow(
                    label = if (isGujarati) "રિમાઇન્ડર સમય" else "Reminder Times",
                    value = medicine.reminderTimes.joinToString(", ")
                )
                DetailRow(
                    label = if (isGujarati) "પુનરાવર્તન" else "Repeat Pattern",
                    value = if (isGujarati) medicine.repeatSchedule.labelGu else medicine.repeatSchedule.labelEn
                )
                DetailRow(
                    label = if (isGujarati) "શરૂઆત તારીખ" else "Start Date",
                    value = medicine.startDate
                )
                if (medicine.notes.isNotBlank()) {
                    DetailRow(
                        label = if (isGujarati) "વિશેષ નોંધ" else "Doctor's Notes",
                        value = medicine.notes
                    )
                }
            }
        }

        // Action Buttons (Edit / Delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onEdit(medicine) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_medicine_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isGujarati) "સુધારો" else "Edit Details")
            }

            Button(
                onClick = { showDeleteConfirm = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("delete_medicine_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isGujarati) "કાઢી નાખો" else "Delete")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isGujarati) "દવા કાઢી નાખવી છે?" else "Delete Medicine?") },
            text = { Text("Are you sure you want to delete ${medicine.name}? All associated scheduled alarms and history logs will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(medicine)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
