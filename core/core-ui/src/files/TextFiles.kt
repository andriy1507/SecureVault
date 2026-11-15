package dev.spaceapps.secvault.core.ui.files

import androidx.compose.runtime.Composable

/** Saves a text file where the user chooses (Android: system file picker, iOS: share sheet → Save to Files). */
fun interface TextFileSaver {
    fun save(
        suggestedName: String,
        content: String,
    )
}

/** Lets the user pick a text file and returns its content, or null if cancelled / unreadable. */
fun interface TextFileOpener {
    fun open()
}

/** What kind of text file is saved or opened. */
data class TextFileKind(
    /** Written as the file's type. */
    val mimeType: String,
    /** Types the open picker offers. */
    val openMimeTypes: List<String>,
    /** Bigger files are rejected (opener returns null). */
    val maxReadBytes: Int,
    /** iOS: plain text vs any data file in the picker. */
    val isPlainText: Boolean,
)

/** [onResult] true when the file was written. */
@Composable
expect fun rememberTextFileSaver(
    kind: TextFileKind,
    onResult: (Boolean) -> Unit,
): TextFileSaver

@Composable
expect fun rememberTextFileOpener(
    kind: TextFileKind,
    onResult: (String?) -> Unit,
): TextFileOpener

/** MIME / UTI-agnostic constants for recovery-key files. */
object TextFileFormat {
    const val MIME_TYPE = "text/plain"
    const val EXTENSION = ".txt"

    /** Recovery key files are tiny; anything bigger is not one. */
    const val MAX_READ_BYTES = 64 * 1024

    val RecoveryKey = TextFileKind(MIME_TYPE, listOf(MIME_TYPE), MAX_READ_BYTES, isPlainText = true)
}

/** Encrypted vault backups (JSON inside, custom extension so pickers may report any type). */
object BackupFileFormat {
    const val MIME_TYPE = "application/json"

    /** Enough for many thousands of items. */
    const val MAX_READ_BYTES = 20 * 1024 * 1024

    val Backup = TextFileKind(
        mimeType = MIME_TYPE,
        openMimeTypes = listOf(MIME_TYPE, "application/octet-stream", "*/*"),
        maxReadBytes = MAX_READ_BYTES,
        isPlainText = false,
    )
}
