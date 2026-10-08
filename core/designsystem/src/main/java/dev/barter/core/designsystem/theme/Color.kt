package dev.barter.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Ported from the prototype's CSS custom properties.
 *
 * `outline` carries `--line` and `onSurfaceVariant` carries `--muted`, so no extra colour roles are
 * needed for the borders and secondary text the design uses.
 */
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2B44FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171A2C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F4F3),
    onBackground = Color(0xFF18201D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18201D),
    surfaceVariant = Color(0xFFDDE3E0),
    onSurfaceVariant = Color(0xFF667470),
    outline = Color(0xFFDDE3E0),
    outlineVariant = Color(0xFFC3CAC7),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8C9AFF),
    onPrimary = Color(0xFF0B0F1A),
    primaryContainer = Color(0xFF1B2C99),
    onPrimaryContainer = Color(0xFFDEE1FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE8EEEB),
    surface = Color(0xFF171C1A),
    onSurface = Color(0xFFE8EEEB),
    surfaceVariant = Color(0xFF262E2B),
    onSurfaceVariant = Color(0xFF8C9A95),
    outline = Color(0xFF262E2B),
    outlineVariant = Color(0xFF3A433F),
)
