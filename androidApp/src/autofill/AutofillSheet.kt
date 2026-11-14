package dev.spaceapps.secvault.autofill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.core.ui.components.AppLogo
import dev.spaceapps.secvault.core.ui.components.AppTextButton
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.ItemAvatar
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.avatarColorFor
import dev.spaceapps.secvault.core.ui.components.avatarLetter
import dev.spaceapps.secvault.core.ui.theme.AppTheme
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem

/** What the autofill window shows. Unlocking shows nothing: only the system unlock prompt is visible. */
internal sealed interface AutofillScreen {
    data object Unlocking : AutofillScreen
    data class Picker(val targetTitle: String, val items: List<VaultItem>) : AutofillScreen
    data class Message(val text: String) : AutofillScreen
}

/**
 * Vault-styled bottom sheet over the other app: app mark and name on top, logins as the vault list
 * shows them. Tapping outside the sheet cancels.
 */
@OptIn(ExperimentalMaterial3Api::class) // BottomSheetDefaults.DragHandle
@Composable
internal fun AutofillSheet(screen: AutofillScreen, onPick: (VaultItem) -> Unit, onCancel: () -> Unit) {
    if (screen == AutofillScreen.Unlocking) return
    val noRipple = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().statusBarsPadding().clickable(interactionSource = noRipple, indication = null, onClick = onCancel),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            // Consumes taps so they don't reach the dismiss area behind.
            modifier = Modifier.fillMaxWidth().clickable(interactionSource = noRipple, indication = null) {},
            shape = RoundedCornerShape(topStart = Dimens.radiusExtraLarge, topEnd = Dimens.radiusExtraLarge),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = Dimens.dp16).padding(bottom = Dimens.dp8),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
            ) {
                BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                when (screen) {
                    is AutofillScreen.Picker -> Picker(screen, onPick)
                    is AutofillScreen.Message -> HeroMessage(
                        icon = Icons.Filled.ErrorOutline,
                        title = stringResource(R.string.autofill_unlock_failed_title),
                        body = screen.text,
                        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    AutofillScreen.Unlocking -> Unit
                }
                AppTextButton(
                    text = stringResource(if (screen is AutofillScreen.Message) R.string.autofill_close else R.string.autofill_cancel),
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun Picker(screen: AutofillScreen.Picker, onPick: (VaultItem) -> Unit) {
    val empty = screen.items.isEmpty()
    SheetHeader(
        stringResource(if (empty) R.string.autofill_no_login else R.string.autofill_choose_login, screen.targetTitle),
    )
    if (empty) {
        HeroMessage(
            icon = Icons.Filled.Key,
            title = stringResource(R.string.autofill_no_login, screen.targetTitle),
            body = stringResource(R.string.autofill_no_login_hint),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        return
    }
    val palette = AppTheme.extendedColors.avatars
    ListGroup {
        LazyColumn {
            itemsIndexed(screen.items, key = { _, item -> item.id }) { index, item ->
                if (index > 0) ListDivider()
                ListRow(
                    title = item.title,
                    subtitle = item.username.takeIf { it.isNotBlank() },
                    leading = { ItemAvatar(avatarColorFor(item.title, palette), letter = avatarLetter(item.title)) },
                    onClick = { onPick(item) },
                )
            }
        }
    }
}

/** App mark + "Vault" + what the sheet is for, so it's clear which app is asking. */
@Composable
private fun SheetHeader(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        AppLogo(
            containerSize = Dimens.sheetLogoContainer,
            iconSize = Dimens.sheetLogoIcon,
            cornerRadius = Dimens.radiusMedium,
        )
        Column {
            Text(stringResource(R.string.autofill_sheet_title), style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
