package dev.spaceapps.secvault

import androidx.compose.runtime.Composable

/** iOS has no activity recreation. */
@Composable
internal actual fun rememberIsChangingConfigurations(): () -> Boolean = { false }
