package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AivoraDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF002028),
    primaryContainer = Color(0xFF004D5C),
    onPrimaryContainer = Color(0xFFB5F4FF),
    secondary = BullishGreen,
    onSecondary = Color(0xFF003915),
    secondaryContainer = BullishGreenSoft,
    onSecondaryContainer = Color(0xFF6BFF9E),
    tertiary = DemoAmber,
    onTertiary = Color(0xFF452B00),
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = BearishRed,
    onError = Color.White,
    errorContainer = BearishRedSoft,
    onErrorContainer = Color(0xFFFFB4AB)
)

@Composable
fun AivoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AivoraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
