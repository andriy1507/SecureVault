package dev.spaceapps.secvault.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.spaceapps.secvault.core.ui.resources.Res
import dev.spaceapps.secvault.core.ui.resources.action_back
import dev.spaceapps.secvault.core.ui.resources.action_close
import dev.spaceapps.secvault.core.ui.resources.strength_fair
import dev.spaceapps.secvault.core.ui.resources.strength_strong
import dev.spaceapps.secvault.core.ui.resources.strength_very_strong
import dev.spaceapps.secvault.core.ui.resources.strength_weak
import dev.spaceapps.secvault.core.ui.theme.AppTheme
import dev.spaceapps.secvault.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

enum class NavIcon { None, Back, Close }

/** Small top app bar with an optional back / close button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navIcon: NavIcon = NavIcon.None,
    onNavigate: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title, color = contentColor) },
        navigationIcon = {
            when (navIcon) {
                NavIcon.None -> Unit
                NavIcon.Back -> IconButton(onClick = onNavigate) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back), tint = contentColor)
                }
                NavIcon.Close -> IconButton(onClick = onNavigate) {
                    Icon(Icons.Filled.Close, stringResource(Res.string.action_close), tint = contentColor)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
    )
}

/** Compact filled "Save" button for top bars. */
@Composable
fun TopBarButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.padding(end = Dimens.dp8).height(Dimens.buttonHeight),
    ) { Text(text) }
}

@Composable
fun BarIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, tint: Color = Color.Unspecified) {
    IconButton(onClick = onClick) {
        if (tint == Color.Unspecified) Icon(icon, contentDescription) else Icon(icon, contentDescription, tint = tint)
    }
}

/** Password strength, from weakest to strongest. */
enum class StrengthLevel(val filledSegments: Int) { Weak(1), Fair(2), Strong(3), VeryStrong(4) }

const val STRENGTH_SEGMENTS = 4

@Composable
fun strengthColor(level: StrengthLevel): Color = when (level) {
    StrengthLevel.Weak -> MaterialTheme.colorScheme.error
    StrengthLevel.Fair -> AppTheme.extendedColors.warning
    StrengthLevel.Strong, StrengthLevel.VeryStrong -> AppTheme.extendedColors.success
}

@Composable
fun strengthLabel(level: StrengthLevel): String = stringResource(
    when (level) {
        StrengthLevel.Weak -> Res.string.strength_weak
        StrengthLevel.Fair -> Res.string.strength_fair
        StrengthLevel.Strong -> Res.string.strength_strong
        StrengthLevel.VeryStrong -> Res.string.strength_very_strong
    },
)

/** Segmented meter plus a "Very strong" label, as on the generator. */
@Composable
fun StrengthMeter(level: StrengthLevel, modifier: Modifier = Modifier) {
    val color = strengthColor(level)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        SegmentBar(
            count = STRENGTH_SEGMENTS,
            filled = level.filledSegments,
            height = Dimens.strengthBarHeight,
            filledColor = color,
            emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        StrengthLabel(level)
    }
}

/** Shield icon + strength label. */
@Composable
fun StrengthLabel(level: StrengthLevel, modifier: Modifier = Modifier) {
    val color = strengthColor(level)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = color, modifier = Modifier.size(Dimens.dp16))
        Text(strengthLabel(level), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun FullWidthRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp8), content = content)
}
