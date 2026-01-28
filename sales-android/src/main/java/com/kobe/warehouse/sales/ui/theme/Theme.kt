package com.kobe.warehouse.sales.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme for Pharma Smart app
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnBackgroundLight,

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnBackgroundLight,

    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnBackgroundLight,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainer,
    onErrorContainer = OnBackgroundLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,

    outline = OutlineLight
)

/**
 * Dark color scheme for Pharma Smart app
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnBackgroundDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnBackgroundDark,

    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnBackgroundDark,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainer,
    onErrorContainer = OnBackgroundDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,

    outline = OutlineDark
)

/**
 * Pharma Smart theme for Jetpack Compose
 *
 * Supports:
 * - Light and dark modes
 * - Dynamic color (Android 12+)
 * - Material Design 3
 *
 * @param darkTheme Whether to use dark theme (default: system setting)
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content Composable content to theme
 */
@Composable
fun PharmaSmartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic color if available (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use dark theme
        darkTheme -> DarkColorScheme
        // Use light theme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
