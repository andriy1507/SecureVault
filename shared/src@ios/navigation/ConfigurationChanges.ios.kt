package dev.spaceapps.secvault.navigation

import androidx.compose.runtime.Composable

/** iOS has no activity recreation. */
@Composable
internal actual fun rememberIsChangingConfigurations(): () -> Boolean = { false }
