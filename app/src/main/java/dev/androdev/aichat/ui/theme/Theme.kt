package dev.androdev.aichat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5F52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9EDE3),
    onPrimaryContainer = Color(0xFF00382F),
    inversePrimary = Color(0xFF7FD8C8),
    secondary = Color(0xFF4B605A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8E1),
    onSecondaryContainer = Color(0xFF0B1F1A),
    tertiary = Color(0xFF6B5B3E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E3C6),
    onTertiaryContainer = Color(0xFF241A05),
    background = Color(0xFFF4F7F6),
    onBackground = Color(0xFF121816),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF121816),
    surfaceVariant = Color(0xFFE3EAE7),
    onSurfaceVariant = Color(0xFF47534F),
    surfaceTint = Color(0xFF0F5F52),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4F2),
    surfaceContainer = Color(0xFFEAF0ED),
    surfaceContainerHigh = Color(0xFFE5ECE9),
    surfaceContainerHighest = Color(0xFFDFE8E4),
    inverseSurface = Color(0xFF2B322F),
    inverseOnSurface = Color(0xFFEFF4F1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF7E8C87),
    outlineVariant = Color(0xFFCBD8D3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8C8),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0B4F45),
    onPrimaryContainer = Color(0xFFB6F2E5),
    inversePrimary = Color(0xFF0F5F52),
    secondary = Color(0xFFB5CCC4),
    onSecondary = Color(0xFF1F332D),
    secondaryContainer = Color(0xFF334741),
    onSecondaryContainer = Color(0xFFD3E8E1),
    tertiary = Color(0xFFD8C5A1),
    onTertiary = Color(0xFF3B2F16),
    tertiaryContainer = Color(0xFF53452A),
    onTertiaryContainer = Color(0xFFF2E3C6),
    background = Color(0xFF0C1211),
    onBackground = Color(0xFFE2EAE6),
    surface = Color(0xFF111917),
    onSurface = Color(0xFFE2EAE6),
    surfaceVariant = Color(0xFF25302C),
    onSurfaceVariant = Color(0xFFB2C0BA),
    surfaceTint = Color(0xFF7FD8C8),
    surfaceContainerLowest = Color(0xFF0A100F),
    surfaceContainerLow = Color(0xFF151E1B),
    surfaceContainer = Color(0xFF19221F),
    surfaceContainerHigh = Color(0xFF1C2622),
    surfaceContainerHighest = Color(0xFF222D29),
    inverseSurface = Color(0xFFE2EAE6),
    inverseOnSurface = Color(0xFF26322D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF7C8B85),
    outlineVariant = Color(0xFF33403B),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AiChatTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
