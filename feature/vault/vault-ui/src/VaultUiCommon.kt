package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextOverflow
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.VaultItemSummary
import dev.spaceapps.secvault.core.ui.components.ItemAvatar
import dev.spaceapps.secvault.core.ui.components.avatarColorFor
import dev.spaceapps.secvault.core.ui.components.avatarLetter
import dev.spaceapps.secvault.core.ui.theme.AppTheme
import dev.spaceapps.secvault.core.ui.theme.ColorPair
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.edited_days_ago
import dev.spaceapps.secvault.feature.vault.resources.edited_today
import dev.spaceapps.secvault.feature.vault.resources.edited_yesterday
import dev.spaceapps.secvault.feature.vault.resources.field_card_number
import dev.spaceapps.secvault.feature.vault.resources.field_cardholder
import dev.spaceapps.secvault.feature.vault.resources.field_cvv
import dev.spaceapps.secvault.feature.vault.resources.field_expiry
import dev.spaceapps.secvault.feature.vault.resources.field_history
import dev.spaceapps.secvault.feature.vault.resources.field_notes
import dev.spaceapps.secvault.feature.vault.resources.field_password
import dev.spaceapps.secvault.feature.vault.resources.field_totp_code
import dev.spaceapps.secvault.feature.vault.resources.field_username
import dev.spaceapps.secvault.feature.vault.resources.field_website
import dev.spaceapps.secvault.feature.vault.resources.subtitle_card
import dev.spaceapps.secvault.feature.vault.resources.subtitle_card_no_expiry
import dev.spaceapps.secvault.feature.vault.resources.subtitle_note
import dev.spaceapps.secvault.feature.vault.resources.type_card
import dev.spaceapps.secvault.feature.vault.resources.type_login
import dev.spaceapps.secvault.feature.vault.resources.type_note_short
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val ItemType.icon: ImageVector
    get() = when (this) {
        ItemType.Login -> Icons.Outlined.Key
        ItemType.Card -> Icons.Outlined.CreditCard
        ItemType.Note -> Icons.AutoMirrored.Outlined.StickyNote2
    }

internal val ItemType.label: StringResource
    get() = when (this) {
        ItemType.Login -> Res.string.type_login
        ItemType.Card -> Res.string.type_card
        ItemType.Note -> Res.string.type_note_short
    }

internal val CopyField.label: StringResource
    get() = when (this) {
        CopyField.Username -> Res.string.field_username
        CopyField.Password -> Res.string.field_password
        CopyField.Totp -> Res.string.field_totp_code
        CopyField.Website -> Res.string.field_website
        CopyField.CardNumber -> Res.string.field_card_number
        CopyField.CardCvv -> Res.string.field_cvv
        CopyField.CardholderName -> Res.string.field_cardholder
        CopyField.CardExpiry -> Res.string.field_expiry
        CopyField.Notes -> Res.string.field_notes
        CopyField.History -> Res.string.field_history
    }

/** Logins get a letter avatar; cards and notes get their type icon, as in the design. */
@Composable
internal fun avatarColors(
    title: String,
    type: ItemType,
): ColorPair {
    val palette = AppTheme.extendedColors.avatars
    return when (type) {
        ItemType.Login -> avatarColorFor(title, palette)
        ItemType.Card -> palette[CARD_AVATAR_INDEX]
        ItemType.Note -> palette[NOTE_AVATAR_INDEX]
    }
}

private const val CARD_AVATAR_INDEX = 3
private const val NOTE_AVATAR_INDEX = 4

@Composable
internal fun TypeAvatar(
    title: String,
    type: ItemType,
    large: Boolean = false,
) {
    val size = if (large) Dimens.avatarLarge else Dimens.avatar
    val letter = avatarLetter(title)
    if (type == ItemType.Login && letter != null) {
        ItemAvatar(avatarColors(title, type), letter = letter, size = size, large = large)
    } else {
        ItemAvatar(avatarColors(title, type), icon = type.icon, size = size)
    }
}

@Composable
internal fun summarySubtitle(item: VaultItemSummary): String =
    when (item.type) {
        ItemType.Login -> item.subtitle

        ItemType.Card -> if (item.subtitle.isNotBlank()) {
            stringResource(Res.string.subtitle_card, item.subtitle)
        } else {
            stringResource(Res.string.subtitle_card_no_expiry)
        }

        ItemType.Note -> stringResource(Res.string.subtitle_note)
    }

/** Filled card row of the vault list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VaultCard(
    item: VaultItemSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailing: @Composable () -> Unit,
    subtitle: String = summarySubtitle(item),
) {
    Surface(
        shape = RoundedCornerShape(Dimens.radiusMedium),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusMedium))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            Modifier
                .heightIn(min = Dimens.vaultCardMinHeight)
                .padding(start = Dimens.dp16, end = Dimens.dp8, top = Dimens.dp12, bottom = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
        ) {
            TypeAvatar(item.title, item.type)
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing()
        }
    }
}

/** "today", "yesterday", "5 days ago". */
@OptIn(ExperimentalTime::class)
@Composable
internal fun relativeDays(epochMillis: Long): String {
    val days = ((Clock.System.now().toEpochMilliseconds() - epochMillis) / 1.days.inWholeMilliseconds).toInt()
    return when {
        days <= 0 -> stringResource(Res.string.edited_today)
        days == 1 -> stringResource(Res.string.edited_yesterday)
        else -> stringResource(Res.string.edited_days_ago, days)
    }
}

private const val HTTPS_PREFIX = "https://"
private const val SCHEME_SEPARATOR = "://"

/** Opens a website field value, adding https:// when the user left it out. */
internal fun UriHandler.openWebsite(url: String) {
    val full = if (SCHEME_SEPARATOR in url) url else HTTPS_PREFIX + url
    runCatching { openUri(full) }
}

/** "https://github.com/login" → "github.com". */
internal fun displayHost(url: String): String =
    url.substringAfter(SCHEME_SEPARATOR).substringBefore('/').ifBlank { url }
