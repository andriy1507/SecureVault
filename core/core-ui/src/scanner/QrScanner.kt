package dev.spaceapps.secvault.core.ui.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Camera permission for the QR scanner. */
interface CameraPermission {
    val granted: Boolean

    /** Shows the system prompt (or does nothing if the user already refused for good). */
    fun request()
}

@Composable
expect fun rememberCameraPermission(): CameraPermission

/**
 * Live camera preview that reports the text of the first QR code it sees.
 * Decoding happens on the device; frames never leave it.
 */
@Composable
expect fun QrScannerView(torchOn: Boolean, onResult: (String) -> Unit, modifier: Modifier = Modifier)
