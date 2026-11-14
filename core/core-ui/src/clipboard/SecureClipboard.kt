package dev.spaceapps.secvault.core.ui.clipboard

import androidx.compose.runtime.Composable

/**
 * Copies text and clears it from the clipboard after a delay.
 * Android 13+: sensitive copies are hidden from the clipboard preview.
 * iOS: copies stay on this device (no Universal Clipboard) and expire.
 */
interface SecureClipboard {
    /** [clearAfterSeconds] == null: leave it on the clipboard. */
    fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int?,
    )
}

@Composable
expect fun rememberSecureClipboard(): SecureClipboard
