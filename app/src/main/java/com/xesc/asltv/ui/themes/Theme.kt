package com.xesc.asltv.ui.themes

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary          = AccentGold,
    secondary        = AccentBlue,
    tertiary         = AccentPurple,
    background       = BackgroundDark,
    surface          = SurfaceCard,
    surfaceVariant   = SurfaceElevated,
    onPrimary        = Color.Black,
    onSecondary      = Color.White,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    outline          = DividerColor
)

val AceStreamTypography = Typography(
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp, color = TextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 22.sp, color = TextPrimary),
    headlineSmall  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    titleMedium    = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, color = TextPrimary),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, color = TextPrimary),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, color = TextSecondary),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 11.sp, color = TextMuted),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AccentGold),
)

@Composable
fun AceStreamTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography   = AceStreamTypography,
        content      = content
    )
}