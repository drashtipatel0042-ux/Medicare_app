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
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayMedicinesScreen(
    todayLogs: List<DoseLog>,
    medicines: List<Medicine>,
    isGujarati: Boolean,
    onTakeDose: (DoseLog) -> Unit,
    onSnoozeDose: (DoseLog) -> Unit,
    onSpeakReminder: (String, String) -> Unit,
    onSelectMedicine: (Medicine) -> Unit,
    onNavigateToAddMedicine: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = if (isGujarati) {
        listOf("બધી (All)", "બાકી (Pending)", "લીધેલી (Taken)", "ચૂકી ગયેલ (Missed)")
    } else {
        listOf("All", "Pending", "Taken", "Missed")
    }

    val filteredLogs = when (selectedTab) {
        1 -> todayLogs.filter { it.status == DoseStatus.PENDING }
        2 -> todayLogs.filter { it.status == DoseStatus.TAKEN }
        3 -> todayLogs.filter { it.status == DoseStatus.MISSED }
        else -> todayLogs
    }

    val takenCount = todayLogs.count { it.status == DoseStatus.TAKEN }
    val totalCount = todayLogs.size

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddMedicine,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_medicine")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Medicine")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("today_medicines_screen")
        ) {
            // Header stats
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isGujarati) "આજની દવાઓનું શિડ્યુલ" else "Today's Medicine Schedule",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$takenCount of $totalCount doses taken today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (totalCount > 0) "${((takenCount.toFloat() / totalCount) * 100).toInt()}% Done" else "100%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab bar
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
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isGujarati) "કોઈ દવા બાકી નથી!" else "No medicines found in this view.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs) { log ->
                        val med = medicines.find { it.id == log.medicineId }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (log.status == DoseStatus.TAKEN) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { med?.let { onSelectMedicine(it) } }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (log.status) {
                                                    DoseStatus.TAKEN -> Color(0xFFD7F8E8)
                                                    DoseStatus.MISSED -> Color(0xFFFFDAD6)
                                                    else -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (log.status) {
                                                DoseStatus.TAKEN -> Icons.Default.Check
                                                DoseStatus.MISSED -> Icons.Default.Close
                                                else -> Icons.Default.Medication
                                            },
                                            contentDescription = null,
                                            tint = when (log.status) {
                                                DoseStatus.TAKEN -> Color(0xFF006A6A)
                                                DoseStatus.MISSED -> Color(0xFFBA1A1A)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.medicineName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${log.dosage} • ${med?.type?.labelEn ?: "Tablet"} • ${med?.foodTiming?.labelEn ?: "After Food"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "⏰ Scheduled: ${log.scheduledTime}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Voice Speaker Button
                                    IconButton(
                                        onClick = { onSpeakReminder(log.medicineName, log.scheduledTime) },
                                        modifier = Modifier.testTag("speak_med_${log.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Voice Reminder",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (log.status == DoseStatus.TAKEN) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = if (isGujarati) "દવા લીધી ✓" else "Completed ✓",
                                                color = Color(0xFF2E7D32),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onSnoozeDose(log) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(text = if (isGujarati) "૧૦ મિ. સ્નૂઝ" else "Snooze 10m", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = { onTakeDose(log) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("mark_taken_btn_${log.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = if (isGujarati) "દવા લીધી" else "Mark Taken", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
