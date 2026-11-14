package dev.spaceapps.secvault.core.ui.files

import androidx.compose.runtime.Composable

/** Saves a text file where the user chooses (Android: system file picker, iOS: share sheet → Save to Files). */
fun interface TextFileSaver {
    fun save(suggestedName: String, content: String)
}

/** Lets the user pick a text file and returns its content, or null if cancelled / unreadable. */
fun interface TextFileOpener {
    fun open()
}

/** [onResult] true when the file was written. */
@Composable
expect fun rememberTextFileSaver(onResult: (Boolean) -> Unit): TextFileSaver

@Composable
expect fun rememberTextFileOpener(onResult: (String?) -> Unit): TextFileOpener

/** MIME / UTI-agnostic constants for recovery-key files. */
object TextFileFormat {
    const val MIME_TYPE = "text/plain"
    const val EXTENSION = ".txt"
    /** Recovery key files are tiny; anything bigger is not one. */
    const val MAX_READ_BYTES = 64 * 1024
}
