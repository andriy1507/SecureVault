package dev.spaceapps.secvault.core.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTextFileSaver(onResult: (Boolean) -> Unit): TextFileSaver {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val pending = remember { arrayOfNulls<String>(1) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TextFileFormat.MIME_TYPE)) { uri ->
            val content = pending[0]
            pending[0] = null
            if (uri == null || content == null) {
                callback.value(false)
                return@rememberLauncherForActivityResult
            }
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) } != null
            }.getOrDefault(false)
            callback.value(ok)
        }
    return remember(launcher) {
        TextFileSaver { name, content ->
            pending[0] = content
            launcher.launch(name)
        }
    }
}

@Composable
actual fun rememberTextFileOpener(onResult: (String?) -> Unit): TextFileOpener {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            callback.value(null)
            return@rememberLauncherForActivityResult
        }
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytes(TextFileFormat.MAX_READ_BYTES)
                bytes.decodeToString()
            }
        }.getOrNull()
        callback.value(text)
    }
    return remember(launcher) { TextFileOpener { launcher.launch(arrayOf(TextFileFormat.MIME_TYPE)) } }
}
