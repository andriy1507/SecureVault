package dev.spaceapps.secvault.feature.lock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.spaceapps.secvault.core.ui.components.BottomActions
import dev.spaceapps.secvault.core.ui.components.ErrorText
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.PrimaryButton
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.cd_biometric
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.Error
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.Intent
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.State
import dev.spaceapps.secvault.feature.lock.resources.Res
import dev.spaceapps.secvault.feature.lock.resources.account_id_mask
import dev.spaceapps.secvault.feature.lock.resources.lock_error_failed
import dev.spaceapps.secvault.feature.lock.resources.lock_error_key_invalidated
import dev.spaceapps.secvault.feature.lock.resources.lock_hint
import dev.spaceapps.secvault.feature.lock.resources.lock_title
import dev.spaceapps.secvault.feature.lock.resources.lock_unlock
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

private const val ID_SEPARATOR = "-"

@Composable
fun LockScreen(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    // Show the OS prompt right away; the buttons are there for retries.
    LaunchedEffect(Unit) { onIntent(Intent.Unlock) }
    LockContent(state, onIntent)
}

/** 15 · Unlock — phone unlock (device credential / biometrics) via the system prompt. */
@Composable
internal fun LockContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            HeroMessage(
                icon = Icons.Filled.Lock,
                title = stringResource(Res.string.lock_title),
                body = state.accountId?.let { maskAccountId(it, stringResource(Res.string.account_id_mask)) },
            ) {
                if (!state.keyInvalidated) {
                    FilledTonalIconButton(
                        onClick = { onIntent(Intent.Unlock) },
                        enabled = !state.isUnlocking,
                        shape = RoundedCornerShape(Dimens.radiusBiometricButton),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.padding(top = Dimens.dp16).size(Dimens.biometricButton),
                    ) {
                        Icon(
                            Icons.Filled.Fingerprint,
                            contentDescription = stringResource(CoreRes.string.cd_biometric),
                            modifier = Modifier.size(Dimens.biometricIcon),
                        )
                    }
                    Text(
                        stringResource(Res.string.lock_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val error = when (val e = state.error) {
                    null -> null
                    Error.KeyInvalidated -> stringResource(Res.string.lock_error_key_invalidated)
                    is Error.Failed -> stringResource(Res.string.lock_error_failed, e.reason.orEmpty())
                }
                ErrorText(error, textAlign = TextAlign.Center)
            }
        }
        if (!state.keyInvalidated) {
            BottomActions {
                PrimaryButton(
                    stringResource(Res.string.lock_unlock),
                    { onIntent(Intent.Unlock) },
                    enabled = !state.isUnlocking,
                )
            }
        }
    }
}

/** "7F3A-91C2-E04B-D58E" → "7F3A-••••-••••-D58E". IDs without groups are shown as they are. */
private fun maskAccountId(
    id: String,
    mask: String,
): String {
    val groups = id.split(ID_SEPARATOR)
    if (groups.size < 3) return id
    return (listOf(groups.first()) + List(groups.size - 2) { mask } + groups.last()).joinToString(ID_SEPARATOR)
}
