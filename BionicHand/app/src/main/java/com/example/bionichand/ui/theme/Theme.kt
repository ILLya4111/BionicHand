package com.example.bionichand.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ColorDarkSurface = Color(0xFF15242B)
private val ColorDarkSurfaceAlt = Color(0xFF20343C)
private val ColorDarkMuted = Color(0xFFB5C7CE)
private val ColorDarkBorder = Color(0xFF35505A)

private val MedicalLightColorScheme = lightColorScheme(
    primary = MedicalPrimary,
    onPrimary = MedicalOnPrimary,
    secondary = MedicalAccent,
    onSecondary = MedicalOnPrimary,
    background = MedicalBackground,
    onBackground = MedicalText,
    surface = MedicalSurface,
    onSurface = MedicalText,
    surfaceVariant = MedicalSurfaceAlt,
    onSurfaceVariant = MedicalTextMuted,
    error = MedicalDanger,
    outline = MedicalBorder
)

private val MedicalDarkColorScheme = darkColorScheme(
    primary = MedicalAccent,
    onPrimary = MedicalOnPrimary,
    secondary = MedicalPrimary,
    background = MedicalText,
    onBackground = MedicalBackground,
    surface = ColorDarkSurface,
    onSurface = MedicalBackground,
    surfaceVariant = ColorDarkSurfaceAlt,
    onSurfaceVariant = ColorDarkMuted,
    error = MedicalDanger,
    outline = ColorDarkBorder
)

@Composable
fun BionicHandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MedicalDarkColorScheme else MedicalLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
