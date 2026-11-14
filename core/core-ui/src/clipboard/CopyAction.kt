package dev.spaceapps.secvault.core.ui.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.spaceapps.secvault.core.ui.components.LocalSnackbarHostState
import dev.spaceapps.secvault.core.ui.resources.Res
import dev.spaceapps.secvault.core.ui.resources.copied
import dev.spaceapps.secvault.core.ui.resources.copied_clears_in
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** Default time before copied secrets are cleared, used until settings say otherwise. */
const val DEFAULT_CLIPBOARD_CLEAR_SECONDS = 30

/** Seconds before copied secrets are cleared from the clipboard; null = never. Set from app settings. */
val LocalClipboardClearSeconds = compositionLocalOf<Int?> { DEFAULT_CLIPBOARD_CLEAR_SECONDS }

/** Copies text and shows "Password copied · clears in 30 s" on the app snackbar host. */
fun interface CopyAction {
    fun copy(
        text: String,
        sensitive: Boolean,
        label: String,
    )
}

@Composable
fun rememberCopyAction(): CopyAction {
    val clipboard = rememberSecureClipboard()
    val snackbar = LocalSnackbarHostState.current
    val clearAfter = LocalClipboardClearSeconds.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, snackbar, clearAfter) {
        CopyAction { text, sensitive, label ->
            val seconds = if (sensitive) clearAfter else null
            clipboard.copy(text, sensitive, seconds)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(
                    if (seconds !=
                        null
                    ) {
                        getString(Res.string.copied_clears_in, label, seconds)
                    } else {
                        getString(Res.string.copied, label)
                    },
                )
            }
        }
    }
}
