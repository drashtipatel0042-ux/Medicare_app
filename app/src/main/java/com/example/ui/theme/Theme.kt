package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MedicalDarkPrimary,
    onPrimary = MedicalDarkOnPrimary,
    primaryContainer = MedicalDarkPrimaryContainer,
    onPrimaryContainer = MedicalDarkOnPrimaryContainer,
    secondary = MedicalDarkSecondary,
    onSecondary = MedicalDarkOnSecondary,
    secondaryContainer = MedicalDarkSecondaryContainer,
    onSecondaryContainer = MedicalDarkOnSecondaryContainer,
    tertiary = MedicalDarkTertiary,
    background = MedicalDarkBackground,
    surface = MedicalDarkSurface,
    surfaceVariant = MedicalDarkSurfaceVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = MedicalBlueOnPrimary,
    primaryContainer = MedicalBlueContainer,
    onPrimaryContainer = MedicalBlueOnContainer,
    secondary = MedicalTealSecondary,
    onSecondary = MedicalTealOnSecondary,
    secondaryContainer = MedicalTealContainer,
    onSecondaryContainer = MedicalTealOnContainer,
    tertiary = MedicalHeartRedTertiary,
    background = MedicalBackground,
    surface = MedicalSurface,
    surfaceVariant = MedicalSurfaceVariant,
    outline = MedicalOutline
  )

@Composable
fun MedicareTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep medical brand colors consistent
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = MedicareTheme(darkTheme, dynamicColor, content)

