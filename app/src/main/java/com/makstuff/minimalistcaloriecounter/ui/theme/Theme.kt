package com.makstuff.minimalistcaloriecounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

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
    primaryContainer = Color(0xFF204B39),
    onPrimaryContainer = Color(0xFFD9F7E8),
    inversePrimary = LightStrong,
    secondary = DarkAmber,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF563700),
    onSecondaryContainer = Color(0xFFFFE5BF),
    tertiary = Color(0xFFC8D9B8),
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF37462D),
    onTertiaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF2D3933),
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

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
