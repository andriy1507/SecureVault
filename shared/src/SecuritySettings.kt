package dev.spaceapps.secvault

import androidx.compose.runtime.Composable

/** Returns an action that opens the system screen-lock settings. */
@Composable
internal expect fun rememberOpenSecuritySettings(): () -> Unit
