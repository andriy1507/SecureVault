package dev.spaceapps.secvault

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberIsChangingConfigurations(): () -> Boolean {
    val activity = LocalActivity.current
    return remember(activity) { { activity?.isChangingConfigurations == true } }
}
