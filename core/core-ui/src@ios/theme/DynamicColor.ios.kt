package dev.spaceapps.secvault.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDynamicColorScheme(dark: Boolean): ColorScheme? = null

actual val isDynamicColorSupported: Boolean = false
