package dev.spaceapps.secvault

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import dev.spaceapps.secvault.resources.Res
import dev.spaceapps.secvault.resources.version_format
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun rememberAppVersion(): String {
    val context = LocalContext.current
    val info = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    return stringResource(
        Res.string.version_format,
        info.versionName.orEmpty(),
        PackageInfoCompat.getLongVersionCode(info).toString(),
    )
}

@Composable
internal actual fun SecureWindow(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    LaunchedEffect(enabled) {
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

internal actual val isScreenshotBlockingSupported: Boolean = true

@Composable
internal actual fun DeviceLockEffect(onLocked: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val callback = rememberUpdatedState(onLocked)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) = callback.value()
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
