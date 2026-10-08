package dev.barter.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/**
 * Dynamic colour is off by default: the design specifies an exact palette, and letting
 * Material You repaint it would replace the accent on every Android 12+ device.
 */
@Composable
fun EvenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EvenTypography,
        shapes = EvenShapes,
        content = content,
    )
}

/** Reads the active scheme rather than the system setting, so an explicit `darkTheme` still works. */
internal val isDarkColorScheme: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

internal val tileBackgroundLightness: Float
    @Composable get() = if (isDarkColorScheme) 0.22f else 0.88f

internal val tileForegroundLightness: Float
    @Composable get() = if (isDarkColorScheme) 0.82f else 0.25f
