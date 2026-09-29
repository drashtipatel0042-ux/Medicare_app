package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyContact
import com.example.data.model.PatientProfile

@Composable
fun EmergencySosScreen(
    contacts: List<EmergencyContact>,
    profile: PatientProfile?,
    isSirenActive: Boolean,
    isGujarati: Boolean,
    onToggleSiren: () -> Unit,
    onDialContact: (String) -> Unit,
    onSendSosSms: (EmergencyContact) -> Unit,
    onAddContact: (String, String, String, Boolean) -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }

    // Pulsing animation for active SOS
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSirenActive) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSirenActive) 500 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emergency_sos_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big Emergency SOS Header & Pulsing Button
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSirenActive) Color(0xFFFFDAD6) else Color(0xFFFFF0F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isGujarati) "ઇમરજન્સી SOS એલર્ટ" else "EMERGENCY SOS ALERT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFBA1A1A)
                        )
                    )
                    Text(
                        text = if (isSirenActive) "⚠️ SIREN & STROBE ACTIVE! ⚠️" else "Tap below in medical emergencies",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFBA1A1A)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pulse SOS Button
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBA1A1A))
                            .clickable { onToggleSiren() }
                            .testTag("sos_main_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isSirenActive) Icons.Default.VolumeOff else Icons.Default.Campaign,
                                contentDescription = "SOS",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                text = if (isSirenActive) "STOP" else "SOS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onDialContact("108") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call 108 Ambulance")
                        }
                    }
                }
            }
        }

        // Family contacts section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isGujarati) "કટોકટી સંપર્કો (Emergency Contacts)" else "Emergency & Family Contacts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Alerts sent if medicine doses are missed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = { showAddContactDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        items(contacts) { contact ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (contact.isPrimarySos) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = if (contact.isPrimarySos) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (contact.isPrimarySos) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFDAD6)
                                ) {
                                    Text(
                                        text = "Primary",
                                        color = Color(0xFFBA1A1A),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${contact.relationship} • ${contact.phoneNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    // Call button
                    IconButton(
                        onClick = { onDialContact(contact.phoneNumber) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // SOS SMS button
                    IconButton(
                        onClick = { onSendSosSms(contact) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = "Send SOS SMS", tint = Color(0xFFC62828))
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var rel by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var isPrimary by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text(if (isGujarati) "કટોકટી સંપર્ક ઉમેરો" else "Add Emergency Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rel,
                        onValueChange = { rel = it },
                        label = { Text("Relationship (e.g. Spouse, Father)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                        Text("Set as Primary SOS Contact")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onAddContact(name, rel, phone, isPrimary)
                    }
                    showAddContactDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
