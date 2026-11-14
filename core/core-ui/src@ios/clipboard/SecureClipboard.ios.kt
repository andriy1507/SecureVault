package dev.spaceapps.secvault.core.ui.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.UIKit.UIPasteboardOptionLocalOnly

private const val PLAIN_TEXT_UTI = "public.utf8-plain-text"

@Composable
actual fun rememberSecureClipboard(): SecureClipboard = remember { IosSecureClipboard() }

private class IosSecureClipboard : SecureClipboard {
    override fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int?,
    ) {
        val options = buildMap<Any?, Any?> {
            put(UIPasteboardOptionLocalOnly, true)
            // The system removes the item itself at the expiration date, even if the app is suspended.
            if (clearAfterSeconds != null) {
                put(UIPasteboardOptionExpirationDate, NSDate.dateWithTimeIntervalSinceNow(clearAfterSeconds.toDouble()))
            }
        }
        UIPasteboard.generalPasteboard.setItems(
            items = listOf(mapOf<Any?, Any?>(PLAIN_TEXT_UTI to text)),
            options = options,
        )
    }
}
