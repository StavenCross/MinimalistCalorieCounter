package com.makstuff.minimalistcaloriecounter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LightStrong,
    onPrimary = LightSurfaceHigh,
    primaryContainer = Color(0xFFDDEFE5),
    onPrimaryContainer = Color(0xFF123A29),
    inversePrimary = Color(0xFFA8E6C8),
    secondary = LightAmber,
    onSecondary = LightBackground,
    secondaryContainer = Color(0xFFFFE5BF),
    onSecondaryContainer = Color(0xFF442700),
    tertiary = Color(0xFF5F6F52),
    onTertiary = LightSurfaceHigh,
    tertiaryContainer = Color(0xFFE3ECCD),
    onTertiaryContainer = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFFECE3D6),
    onSurfaceVariant = LightMutedText,
    surfaceTint = LightStrong,
    inverseSurface = LightText,
    inverseOnSurface = LightBackground,
    error = LightError,
    onError = LightSurfaceHigh,
    errorContainer = LightErrorContainer,
    onErrorContainer = Color(0xFF410002),
    outline = LightWeak,
    outlineVariant = LightShadow,
    scrim = LightText,
    surfaceContainer = LightBackground,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightSurfaceHigh,
)


private val DarkColors = darkColorScheme(
    primary = DarkStrong,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF34373D),
    onPrimaryContainer = Color(0xFFF3F5F7),
    inversePrimary = Color(0xFF4B5563),
    secondary = DarkAmber,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF2E3136),
    onSecondaryContainer = Color(0xFFF1F4F8),
    tertiary = Color(0xFFBFC5CE),
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF30333A),
    onTertiaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF2D3035),
    onSurfaceVariant = DarkMutedText,
    surfaceTint = DarkStrong,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError,
    outline = DarkWeak,
    outlineVariant = DarkShadow,
    scrim = DarkText,
    surfaceContainer = DarkBackground,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkSurfaceHigh,
)

enum class AppTheme {
    MODE_DAY,
    MODE_NIGHT,
    MODE_AUTO;
}

data class DarkTheme(val isDark: Boolean = false)

val LocalTheme = compositionLocalOf { DarkTheme() }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && useDarkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        !useDarkTheme -> LightColors
        else -> DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
