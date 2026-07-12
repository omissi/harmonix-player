package com.alomessi.harmonix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alomessi.harmonix.data.AppSettings

val HarmonixPurple = Color(0xFF7C4DFF)
val HarmonixPink = Color(0xFFE75CFF)
val HarmonixBlue = Color(0xFF35A7FF)
val HarmonixOrange = Color(0xFFFF8A3D)
val HarmonixDark = Color(0xFF0B0D1B)
val HarmonixSurface = Color(0xFF15182B)

private fun accent(settings: AppSettings): Color = when (settings.accent) {
    "blue" -> HarmonixBlue
    "orange" -> HarmonixOrange
    "pink" -> HarmonixPink
    else -> HarmonixPurple
}

@Composable
fun HarmonixTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val primary = accent(settings)
    val colors = if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = .24f),
            onPrimaryContainer = Color.White,
            secondary = HarmonixPink,
            tertiary = HarmonixBlue,
            background = HarmonixDark,
            onBackground = Color(0xFFF4F1FF),
            surface = HarmonixDark,
            surfaceVariant = HarmonixSurface,
            onSurface = Color(0xFFF4F1FF),
            onSurfaceVariant = Color(0xFFBBB8CC),
            outline = Color(0xFF46495D),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = .13f),
            onPrimaryContainer = Color(0xFF27134F),
            secondary = Color(0xFF9B3EAC),
            tertiary = Color(0xFF006BA6),
            background = Color(0xFFFAF8FF),
            onBackground = Color(0xFF1C1B20),
            surface = Color(0xFFFAF8FF),
            surfaceVariant = Color(0xFFF0EDF7),
            onSurface = Color(0xFF1C1B20),
            onSurfaceVariant = Color(0xFF676270),
            outline = Color(0xFF7B7682),
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}
