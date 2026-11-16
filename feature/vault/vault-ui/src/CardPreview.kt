package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import dev.spaceapps.secvault.core.domain.model.CARD_VISIBLE_DIGITS
import dev.spaceapps.secvault.core.domain.model.PaymentCard
import dev.spaceapps.secvault.core.ui.theme.AppTheme
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.card_preview_expiry_placeholder
import dev.spaceapps.secvault.feature.vault.resources.card_preview_name_placeholder
import dev.spaceapps.secvault.feature.vault.resources.card_preview_number_placeholder
import org.jetbrains.compose.resources.stringResource

private const val CARD_GROUP = 4
private const val MASK_GROUP = "••••"
private const val GROUP_SEPARATOR = " "

/** "4417123456781234" → "4417 •••• •••• 1234": first and last group visible. */
internal fun maskCardNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    if (digits.length <=
        CARD_GROUP + CARD_VISIBLE_DIGITS
    ) {
        return digits.chunked(CARD_GROUP).joinToString(GROUP_SEPARATOR)
    }
    val groups = digits.chunked(CARD_GROUP)
    return (listOf(groups.first()) + List(groups.size - 2) { MASK_GROUP } + digits.takeLast(CARD_VISIBLE_DIGITS))
        .joinToString(GROUP_SEPARATOR)
}

internal fun groupCardNumber(number: String): String =
    PaymentCard.groupNumber(number.filter { it.isDigit() }).joinToString(GROUP_SEPARATOR)

/** Gradient payment card from the "New card" design. */
@Composable
internal fun CardPreview(
    name: String,
    number: String,
    holder: String,
    expiry: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.extendedColors
    Box(
        modifier
            .fillMaxWidth()
            .height(Dimens.cardPreviewHeight)
            .clip(RoundedCornerShape(Dimens.radiusCardPreview))
            .background(Brush.linearGradient(listOf(colors.cardGradientStart, colors.cardGradientEnd))),
    ) {
        Column(Modifier.fillMaxSize().padding(Dimens.dp20), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = colors.onCard)
                Icon(Icons.Outlined.Contactless, contentDescription = null, tint = colors.onCard)
            }
            Text(
                if (number.isBlank()) {
                    stringResource(
                        Res.string.card_preview_number_placeholder,
                    )
                } else {
                    maskCardNumber(number)
                },
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                color = colors.onCard,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    holder.ifBlank { stringResource(Res.string.card_preview_name_placeholder) }.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onCard,
                )
                Text(
                    expiry.ifBlank { stringResource(Res.string.card_preview_expiry_placeholder) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onCard,
                )
            }
        }
    }
}
