package dev.spaceapps.secvault

import androidx.compose.runtime.Composable

/**
 * Returns a check that is true while the host is being recreated (rotation, theme change, split screen).
 * Read it inside `onDispose` to tell recreation apart from a real removal.
 */
@Composable
internal expect fun rememberIsChangingConfigurations(): () -> Boolean
