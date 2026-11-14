package dev.spaceapps.secvault.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class ThemeMode { System, Light, Dark }

/** Wallpaper-based colors (Android 12+). Null where the platform has none. */
@Composable
internal expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?

/** Whether [platformDynamicColorScheme] can return colors on this device. */
expect val isDynamicColorSupported: Boolean

@Composable
fun SecureVaultTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val dynamic = if (dynamicColor) platformDynamicColorScheme(dark) else null
    val colors = dynamic ?: if (dark) DarkColors else LightColors
    val extended = if (dark) darkExtendedColors(colors) else lightExtendedColors(colors)
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

/** Shortcut: `AppTheme.extendedColors.success`. */
object AppTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
