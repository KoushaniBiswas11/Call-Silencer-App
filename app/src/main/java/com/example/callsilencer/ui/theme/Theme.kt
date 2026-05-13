package com.example.callsilencer.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = CardDark,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Muted,
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2D3561),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF6B7FFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CardDark,
    onSecondaryContainer = Color(0xFFB8BDCC),
    tertiary = Warning,
    onTertiary = Color(0xFF0A0E27),
    error = Danger,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = CardLight,
    onBackground = Color(0xFF0A0E27),
    onSurface = Color(0xFF0A0E27),
    onSurfaceVariant = MutedLight,
    primary = PrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF0A0E27),
    secondary = PrimaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CardLight,
    onSecondaryContainer = Color(0xFF0A0E27),
    tertiary = Color(0xFFE8873A),
    onTertiary = Color(0xFFFFFFFF),
    error = Danger,
    onError = Color.White,
)

@Composable
fun CallSilencerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // follows phone setting automatically
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val activity = view.context as? Activity

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = if (darkTheme) NavBackground.toArgb()
            else BackgroundLight.toArgb()
        }
    }
}