package dev.spaceapps.secvault.core.ui.navigation

import androidx.compose.runtime.Composable

/** System back (Android). iOS has no back button; screens show their own. */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
