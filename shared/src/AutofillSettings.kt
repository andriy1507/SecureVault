package dev.spaceapps.secvault

import androidx.compose.runtime.Composable

/** Opens the system screen for choosing the autofill provider, or null where there is none (iOS). */
@Composable
internal expect fun rememberAutofillSettingsAction(): (() -> Unit)?

/** Opens the system screen for choosing the passkey provider, or null where Vault can't be one. */
@Composable
internal expect fun rememberPasskeySettingsAction(): (() -> Unit)?
