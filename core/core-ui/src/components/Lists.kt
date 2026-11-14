package dev.spaceapps.secvault.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import dev.spaceapps.secvault.core.ui.theme.ColorPair
import dev.spaceapps.secvault.core.ui.theme.Dimens

/** Rounded container that groups list rows ("Autofill & sync", "Security"…). */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(content = content)
    }
}

@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = Dimens.dp16),
        thickness = Dimens.dividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * One-line or two-line list row.
 * [trailing] defaults to nothing; use [ChevronTrailing] for rows that open a screen.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leadingTint: Color = if (contentColor == MaterialTheme.colorScheme.onSurface) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        contentColor
    },
    monospaceSubtitle: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = Dimens.dp56)
            .padding(horizontal = Dimens.dp16, vertical = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
    ) {
        if (leading != null) {
            leading()
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = leadingTint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = if (monospaceSubtitle) FontFamily.Monospace else null,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ChevronTrailing() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        modifier = modifier,
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
    )
}

/** Labelled value with trailing actions, used on the item detail screen. */
@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.dp56)
            .padding(start = Dimens.dp16, end = Dimens.dp4, top = Dimens.dp8, bottom = Dimens.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (monospace) FontFamily.Monospace else null,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

/** Round avatar with a letter or an icon. */
@Composable
fun ItemAvatar(
    colors: ColorPair,
    modifier: Modifier = Modifier,
    letter: String? = null,
    icon: ImageVector? = null,
    size: androidx.compose.ui.unit.Dp = Dimens.avatar,
    large: Boolean = false,
) {
    Surface(modifier = modifier.size(size), shape = CircleShape, color = colors.container, contentColor = colors.content) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(icon, contentDescription = null)
                letter != null -> Text(
                    letter,
                    style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Picks a stable avatar color for a title, so an item keeps its color between launches. */
fun avatarColorFor(key: String, palette: List<ColorPair>): ColorPair =
    palette[(key.lowercase().hashCode() and Int.MAX_VALUE) % palette.size]

/** First letter of a title, uppercase, or null when it has none. */
fun avatarLetter(title: String): String? =
    title.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString()
