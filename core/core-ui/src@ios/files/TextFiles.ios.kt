@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.spaceapps.secvault.core.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

@Composable
actual fun rememberTextFileSaver(onResult: (Boolean) -> Unit): TextFileSaver {
    val callback = rememberUpdatedState(onResult)
    return remember {
        TextFileSaver { name, content ->
            val url = writeTemp(name, content)
            val presenter = topViewController()
            if (url == null || presenter == null) {
                callback.value(false)
                return@TextFileSaver
            }
            val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
            sheet.completionWithItemsHandler = { _, completed, _, _ ->
                // The file holds the recovery key: never leave it in the temp folder.
                NSFileManager.defaultManager.removeItemAtURL(url, error = null)
                callback.value(completed)
            }
            presenter.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberTextFileOpener(onResult: (String?) -> Unit): TextFileOpener {
    val callback = rememberUpdatedState(onResult)
    val delegate = remember { PickerDelegate { callback.value(it) } }
    return remember {
        TextFileOpener {
            val presenter = topViewController() ?: return@TextFileOpener callback.value(null)
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypePlainText), asCopy = true)
            picker.delegate = delegate
            presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class PickerDelegate(
    private val onResult: (String?) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onResult(url?.let(::readText))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

private fun readText(url: NSURL): String? {
    val data = NSData.dataWithContentsOfURL(url) ?: return null
    if (data.length.toInt() > TextFileFormat.MAX_READ_BYTES) return null
    return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
}

private fun writeTemp(
    name: String,
    content: String,
): NSURL? {
    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + name)

    @Suppress("CAST_NEVER_SUCCEEDS")
    val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return null
    return if (data.writeToURL(url, atomically = true)) url else null
}

private fun topViewController(): UIViewController? {
    @Suppress("DEPRECATION")
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
