package dev.spaceapps.secvault.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import dev.spaceapps.secvault.core.ui.theme.Dimens
import kotlinx.coroutines.flow.Flow

/** App-wide snackbar host, so a message survives the screen that triggered it (e.g. "moved to trash · Undo"). */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

/** Collects one-off MVI effects while the screen is in composition. */
@Composable
fun <E> CollectEffects(
    effects: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    LaunchedEffect(effects) { effects.collect { onEffect(it) } }
}

@Composable
fun FullScreenProgress(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorText(
    message: String?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    if (message != null) {
        Text(
            message,
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = textAlign,
        )
    }
}

/** Big filled pill button at the bottom of a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.fillMaxWidth().height(Dimens.bottomButtonHeight),
    ) { Text(text) }
}

/** Big tonal pill button at the bottom of a screen. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.fillMaxWidth().height(Dimens.bottomButtonHeight),
    ) { ButtonContent(text, icon) }
}

/** Compact tonal button with an optional leading icon ("Copy", "Save as .txt", "Import"). */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.height(Dimens.buttonHeight),
    ) { ButtonContent(text, icon) }
}

@Composable
fun FilledIconTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.height(Dimens.buttonHeight),
    ) { ButtonContent(text, icon) }
}

@Composable
fun OutlinedIconTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.height(Dimens.bottomButtonHeight),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(Dimens.dp18))
            Box(Modifier.size(Dimens.dp8))
        }
        Text(text, color = contentColor)
    }
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, color = color) }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.dp18))
        Box(Modifier.size(Dimens.dp8))
    }
    Text(text)
}

/** Padded column for the buttons at the bottom of a screen. */
@Composable
fun BottomActions(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.dp24, end = Dimens.dp24, top = Dimens.dp16, bottom = Dimens.dp24),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        content = content,
    )
}

/** Round tinted container with a large icon, used on centered "hero" screens. */
@Composable
fun HeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = modifier.size(Dimens.heroIconContainer),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.heroIcon))
        }
    }
}

/**
 * Centered block: hero icon, title, body and optional extra content.
 * Used by empty states, PIN screens, and the steps of the create-account flow.
 */
@Composable
fun HeroMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.heroPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.dp16),
    ) {
        HeroIcon(icon, containerColor = iconContainerColor, contentColor = iconContentColor)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        extra()
    }
}

/** Full-size box that centers [HeroMessage]-like content vertically. */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Small primary-colored section title above a list group ("Favorites", "Security"). */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text,
        modifier = modifier.padding(start = Dimens.dp4, end = Dimens.dp4, top = Dimens.dp8),
        style = MaterialTheme.typography.titleSmall,
        color = color,
    )
}

/** Row of [count] equal segments, the first [filled] in primary color. */
@Composable
fun SegmentBar(
    count: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = Dimens.stepBarHeight,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp6)) {
        repeat(count) { index ->
            Surface(
                modifier = Modifier.weight(1f).height(height),
                shape = CircleShape,
                color = if (index < filled) filledColor else emptyColor,
            ) {}
        }
    }
}

/** App mark: rounded square with a shield, on the welcome and splash screens. */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    containerSize: androidx.compose.ui.unit.Dp = Dimens.logoContainer,
    iconSize: androidx.compose.ui.unit.Dp = Dimens.logoIcon,
    cornerRadius: androidx.compose.ui.unit.Dp = Dimens.radiusLogo,
) {
    Surface(
        modifier = modifier.size(containerSize),
        shape = androidx.compose.foundation.shape
            .RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppLogo(
            containerSize = Dimens.splashLogoContainer,
            iconSize = Dimens.splashLogoIcon,
            cornerRadius = Dimens.radiusSplashLogo,
        )
    }
}
