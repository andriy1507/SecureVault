package dev.spaceapps.secvault.core.ui.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
actual fun rememberSecureClipboard(): SecureClipboard {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidSecureClipboard(context) }
}

private class AndroidSecureClipboard(
    context: Context,
) : SecureClipboard {
    private val manager = context.getSystemService(ClipboardManager::class.java)

    override fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int?,
    ) {
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        if (sensitive) {
            // ClipDescription.EXTRA_IS_SENSITIVE (API 33); the plain key also works on Android 12L builds that honor it
            clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        manager.setPrimaryClip(clip)
        ClearScheduler.schedule(manager, text, clearAfterSeconds)
    }

    private companion object {
        const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    }
}

private const val MILLIS_PER_SECOND = 1000L
private const val CLIP_LABEL = "SecureVault"

/** Process-wide, so the clear still happens after the screen that copied is gone. */
private object ClearScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun schedule(
        manager: ClipboardManager,
        copied: String,
        clearAfterSeconds: Int?,
    ) {
        job?.cancel()
        if (clearAfterSeconds == null) return
        job = scope.launch {
            delay(clearAfterSeconds * MILLIS_PER_SECOND)
            // If we can read it and the user copied something else since, leave it alone.
            // In background Android hides the clipboard from us (null): clear anyway.
            val current = runCatching {
                manager.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString()
            }.getOrNull()
            if (current == null || current == copied) manager.clearPrimaryClip()
        }
    }
}
