package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicareTopBar(
    title: String,
    isGujarati: Boolean,
    userName: String = "",
    onToggleLanguage: () -> Unit,
    onSosClick: () -> Unit,
    onAdminClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Medicare Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = if (userName.isNotBlank() && userName != "Patient") "👤 $userName" else if (isGujarati) "સ્માર્ટ હેલ્થકેર" else "Smart Healthcare",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Language Switcher Badge
            Surface(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .testTag("toggle_language_button")
            ) {
                Text(
                    text = if (isGujarati) "EN" else "ગુજ",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Quick SOS Button
            IconButton(
                onClick = onSosClick,
                modifier = Modifier
                    .testTag("top_bar_sos_button")
                    .clip(CircleShape)
                    .background(Color(0xFFFFDAD6))
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency SOS",
                    tint = Color(0xFFBA1A1A)
                )
            }

            // Admin Dashboard button
            IconButton(
                onClick = onAdminClick,
                modifier = Modifier.testTag("top_bar_admin_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = "Admin Portal"
                )
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("top_bar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            }

            // User Profile / Phone Login button
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag("top_bar_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Account",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun MedicareBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isGujarati: Boolean
) {
    val items = listOf(
        Triple(Screen.Dashboard, Icons.Default.Dashboard, if (isGujarati) "હોમ" else "Home"),
        Triple(Screen.TodayMedicines, Icons.Default.Medication, if (isGujarati) "દવાઓ" else "Medicines"),
        Triple(Screen.HealthTracker, Icons.Default.Favorite, if (isGujarati) "સ્વાસ્થ્ય" else "Vitals"),
        Triple(Screen.AiChat, Icons.Default.SmartToy, if (isGujarati) "AI સહાયક" else "AI Chat"),
        Triple(Screen.EmergencySos, Icons.Default.Sos, if (isGujarati) "SOS" else "SOS")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { (screen, icon, label) ->
            val selected = currentScreen == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("nav_item_${screen.route}"),
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (screen == Screen.EmergencySos && selected) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = if (screen == Screen.EmergencySos) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = if (screen == Screen.EmergencySos) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
