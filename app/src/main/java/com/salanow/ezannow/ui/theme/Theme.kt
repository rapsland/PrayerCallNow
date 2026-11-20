package com.salanow.ezannow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.salanow.ezannow.domain.model.ThemeMode

val EzanEmeraldDeep = Color(0xFF0A3B24)
val EzanEmerald = Color(0xFF146C43)
val EzanEmeraldBright = Color(0xFF1B8A56)
val EzanGold = Color(0xFFF5C451)
val EzanGoldDeep = Color(0xFFB87D08)

private val LightColorScheme = lightColorScheme(
    primary = EzanEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0E2),
    onPrimaryContainer = Color(0xFF062918),
    secondary = EzanGoldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1D0),
    onSecondaryContainer = Color(0xFF3A2400),
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF141D18),
    surface = Color.White,
    onSurface = Color(0xFF141D18),
    surfaceVariant = Color(0xFFE7EFEA),
    onSurfaceVariant = Color(0xFF4F6057),
    outline = Color(0xFFD0DDD6),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5AD094),
    onPrimary = Color(0xFF00381F),
    primaryContainer = Color(0xFF0F5132),
    onPrimaryContainer = Color(0xFFC2F5D9),
    secondary = EzanGold,
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = Color(0xFF463300),
    onSecondaryContainer = Color(0xFFFFE49E),
    background = Color(0xFF0D1411),
    onBackground = Color(0xFFE6EFEA),
    surface = Color(0xFF151F1A),
    onSurface = Color(0xFFE6EFEA),
    surfaceVariant = Color(0xFF1E2B24),
    onSurfaceVariant = Color(0xFF9EB0A6),
    outline = Color(0xFF2D3E35),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Immutable
data class EzanExtendedColors(
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val goldAccent: Color,
    val kerahatContainer: Color,
    val kerahatBorder: Color,
    val kerahatContent: Color,
    val activeCardContainer: Color,
    val activeCardBorder: Color,
    val fridayHighlight: Color,
    val subtleDivider: Color
)

private val LightExtendedColors = EzanExtendedColors(
    heroGradientStart = Color(0xFF146C43),
    heroGradientEnd = Color(0xFF093822),
    goldAccent = EzanGold,
    kerahatContainer = Color(0xFFFFF3DC),
    kerahatBorder = Color(0xFFE8B44A),
    kerahatContent = Color(0xFF664300),
    activeCardContainer = Color(0xFFE5F6ED),
    activeCardBorder = Color(0xFF146C43),
    fridayHighlight = Color(0xFFF0FAF4),
    subtleDivider = Color(0xFFE3ECE7)
)

private val DarkExtendedColors = EzanExtendedColors(
    heroGradientStart = Color(0xFF135C3A),
    heroGradientEnd = Color(0xFF072416),
    goldAccent = EzanGold,
    kerahatContainer = Color(0xFF33260C),
    kerahatBorder = Color(0xFF8C6214),
    kerahatContent = Color(0xFFFFD885),
    activeCardContainer = Color(0xFF133323),
    activeCardBorder = Color(0xFF5AD094),
    fridayHighlight = Color(0xFF12281D),
    subtleDivider = Color(0xFF25342C)
)

val LocalEzanColors = staticCompositionLocalOf { LightExtendedColors }

val CountdownTextStyle = TextStyle(
    fontSize = 38.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    fontFeatureSettings = "tnum"
)

val TabularTimeStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum"
)

private val EzanTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun EzanNowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalEzanColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EzanTypography,
            content = content
        )
    }
}
