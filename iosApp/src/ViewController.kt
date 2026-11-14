package dev.spaceapps.secvault

import androidx.compose.ui.window.ComposeUIViewController
import dev.spaceapps.secvault.core.data.di.iosDataModule
import platform.UIKit.UIViewController

private var koinStarted = false

/** Called once from Swift with the Secure Enclave implementation. */
fun ViewController(keyGuard: NativeKeyGuard): UIViewController {
    if (!koinStarted) {
        initKoin(iosDataModule(IosHardwareKeyGuard(keyGuard)))
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
