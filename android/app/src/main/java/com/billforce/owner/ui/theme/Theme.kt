package com.billforce.owner.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Surface,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDark,
    secondary = Amber,
    onSecondary = Surface,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = AmberDark,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    outline = Border,
    error = RedDanger,
    onError = Surface
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = TealDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Teal80,
    secondary = Amber,
    onSecondary = SurfaceDark,
    background = BackgroundDark,
    onBackground = Surface,
    surface = SurfaceDark,
    onSurface = Surface,
    surfaceVariant = Surface2Dark,
    onSurfaceVariant = TextLight,
    outline = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D)
)

// Helper to avoid import clash with compose Color
private fun Color(hex: Long) = androidx.compose.ui.graphics.Color(hex.toInt())

@Composable
fun BillforceOwnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep brand teal consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BillforceTypography,
        content = content
    )
}
