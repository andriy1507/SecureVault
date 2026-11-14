package dev.spaceapps.secvault.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Material 3 baseline palette generated from seed #4355B9 (see the design file). */
internal object Palette {
    // Light
    val LightPrimary = Color(0xFF4355B9)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFDEE0FF)
    val LightOnPrimaryContainer = Color(0xFF00105C)
    val LightSecondary = Color(0xFF5B5D72)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFE0E1F9)
    val LightOnSecondaryContainer = Color(0xFF181A2C)
    val LightTertiary = Color(0xFF77536D)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFFFD7F1)
    val LightOnTertiaryContainer = Color(0xFF2D1228)
    val LightError = Color(0xFFBA1A1A)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)
    val LightSurface = Color(0xFFFBF8FF)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFF5F2FA)
    val LightSurfaceContainer = Color(0xFFEFEDF4)
    val LightSurfaceContainerHigh = Color(0xFFEAE7EF)
    val LightSurfaceContainerHighest = Color(0xFFE4E1E9)
    val LightOnSurface = Color(0xFF1B1B21)
    val LightOnSurfaceVariant = Color(0xFF46464F)
    val LightOutline = Color(0xFF767680)
    val LightOutlineVariant = Color(0xFFC6C5D0)
    val LightInverseSurface = Color(0xFF303036)
    val LightInverseOnSurface = Color(0xFFF2EFF7)
    val LightInversePrimary = Color(0xFFBAC3FF)

    // Dark
    val DarkPrimary = Color(0xFFBAC3FF)
    val DarkOnPrimary = Color(0xFF08218A)
    val DarkPrimaryContainer = Color(0xFF293CA0)
    val DarkOnPrimaryContainer = Color(0xFFDEE0FF)
    val DarkSecondary = Color(0xFFC4C5DD)
    val DarkOnSecondary = Color(0xFF2D2F42)
    val DarkSecondaryContainer = Color(0xFF424659)
    val DarkOnSecondaryContainer = Color(0xFFE0E1F9)
    val DarkTertiary = Color(0xFFE6BAD7)
    val DarkOnTertiary = Color(0xFF44263D)
    val DarkTertiaryContainer = Color(0xFF5C3F57)
    val DarkOnTertiaryContainer = Color(0xFFFFD7F1)
    val DarkError = Color(0xFFFFB4AB)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkSurface = Color(0xFF131318)
    val DarkSurfaceContainerLowest = Color(0xFF0E0E13)
    val DarkSurfaceContainerLow = Color(0xFF1B1B21)
    val DarkSurfaceContainer = Color(0xFF1F1F25)
    val DarkSurfaceContainerHigh = Color(0xFF2A292F)
    val DarkSurfaceContainerHighest = Color(0xFF35343A)
    val DarkOnSurface = Color(0xFFE4E1E9)
    val DarkOnSurfaceVariant = Color(0xFFC6C5D0)
    val DarkOutline = Color(0xFF90909A)
    val DarkOutlineVariant = Color(0xFF46464F)
    val DarkInverseSurface = Color(0xFFE4E1E9)
    val DarkInverseOnSurface = Color(0xFF303036)
    val DarkInversePrimary = Color(0xFF4355B9)

    // Extended (not part of the M3 scheme)
    val LightSuccess = Color(0xFF1E6C3A)
    val DarkSuccess = Color(0xFF7DDC9A)
    val LightWarning = Color(0xFF8B5000)
    val DarkWarning = Color(0xFFFFB870)
    val LightGreenContainer = Color(0xFFC4EED0)
    val LightOnGreenContainer = Color(0xFF002110)
    val DarkGreenContainer = Color(0xFF07522A)
    val DarkOnGreenContainer = Color(0xFFC4EED0)
    val LightOrangeContainer = Color(0xFFFFDCC1)
    val LightOnOrangeContainer = Color(0xFF2E1500)
    val DarkOrangeContainer = Color(0xFF6A3B00)
    val DarkOnOrangeContainer = Color(0xFFFFDCC1)
    val CardGradientEnd = Color(0xFF6C4DFF)
    val OnCard = Color(0xFFFFFFFF)
    val QrBackground = Color(0xFFFFFFFF)
    val ScannerBackground = Color(0xFF1A1A1F)
}

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Palette.LightPrimary,
    onPrimary = Palette.LightOnPrimary,
    primaryContainer = Palette.LightPrimaryContainer,
    onPrimaryContainer = Palette.LightOnPrimaryContainer,
    secondary = Palette.LightSecondary,
    onSecondary = Palette.LightOnSecondary,
    secondaryContainer = Palette.LightSecondaryContainer,
    onSecondaryContainer = Palette.LightOnSecondaryContainer,
    tertiary = Palette.LightTertiary,
    onTertiary = Palette.LightOnTertiary,
    tertiaryContainer = Palette.LightTertiaryContainer,
    onTertiaryContainer = Palette.LightOnTertiaryContainer,
    error = Palette.LightError,
    onError = Palette.LightOnError,
    errorContainer = Palette.LightErrorContainer,
    onErrorContainer = Palette.LightOnErrorContainer,
    background = Palette.LightSurface,
    onBackground = Palette.LightOnSurface,
    surface = Palette.LightSurface,
    onSurface = Palette.LightOnSurface,
    surfaceVariant = Palette.LightSurfaceContainerHighest,
    onSurfaceVariant = Palette.LightOnSurfaceVariant,
    surfaceContainerLowest = Palette.LightSurfaceContainerLowest,
    surfaceContainerLow = Palette.LightSurfaceContainerLow,
    surfaceContainer = Palette.LightSurfaceContainer,
    surfaceContainerHigh = Palette.LightSurfaceContainerHigh,
    surfaceContainerHighest = Palette.LightSurfaceContainerHighest,
    outline = Palette.LightOutline,
    outlineVariant = Palette.LightOutlineVariant,
    inverseSurface = Palette.LightInverseSurface,
    inverseOnSurface = Palette.LightInverseOnSurface,
    inversePrimary = Palette.LightInversePrimary,
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Palette.DarkPrimary,
    onPrimary = Palette.DarkOnPrimary,
    primaryContainer = Palette.DarkPrimaryContainer,
    onPrimaryContainer = Palette.DarkOnPrimaryContainer,
    secondary = Palette.DarkSecondary,
    onSecondary = Palette.DarkOnSecondary,
    secondaryContainer = Palette.DarkSecondaryContainer,
    onSecondaryContainer = Palette.DarkOnSecondaryContainer,
    tertiary = Palette.DarkTertiary,
    onTertiary = Palette.DarkOnTertiary,
    tertiaryContainer = Palette.DarkTertiaryContainer,
    onTertiaryContainer = Palette.DarkOnTertiaryContainer,
    error = Palette.DarkError,
    onError = Palette.DarkOnError,
    errorContainer = Palette.DarkErrorContainer,
    onErrorContainer = Palette.DarkOnErrorContainer,
    background = Palette.DarkSurface,
    onBackground = Palette.DarkOnSurface,
    surface = Palette.DarkSurface,
    onSurface = Palette.DarkOnSurface,
    surfaceVariant = Palette.DarkSurfaceContainerHighest,
    onSurfaceVariant = Palette.DarkOnSurfaceVariant,
    surfaceContainerLowest = Palette.DarkSurfaceContainerLowest,
    surfaceContainerLow = Palette.DarkSurfaceContainerLow,
    surfaceContainer = Palette.DarkSurfaceContainer,
    surfaceContainerHigh = Palette.DarkSurfaceContainerHigh,
    surfaceContainerHighest = Palette.DarkSurfaceContainerHighest,
    outline = Palette.DarkOutline,
    outlineVariant = Palette.DarkOutlineVariant,
    inverseSurface = Palette.DarkInverseSurface,
    inverseOnSurface = Palette.DarkInverseOnSurface,
    inversePrimary = Palette.DarkInversePrimary,
)

/** A background / content color pair, e.g. for avatars. */
@Immutable
data class ColorPair(
    val container: Color,
    val content: Color,
)

/** Colors the design uses that Material's ColorScheme doesn't have. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    /** Avatar backgrounds, picked per item so the list is easy to scan. */
    val avatars: List<ColorPair>,
    val cardGradientStart: Color,
    val cardGradientEnd: Color,
    val onCard: Color,
    val qrBackground: Color,
    val scannerBackground: Color,
)

internal fun lightExtendedColors(scheme: ColorScheme) =
    ExtendedColors(
        success = Palette.LightSuccess,
        warning = Palette.LightWarning,
        avatars = listOf(
            ColorPair(scheme.primaryContainer, scheme.onPrimaryContainer),
            ColorPair(scheme.tertiaryContainer, scheme.onTertiaryContainer),
            ColorPair(Palette.LightGreenContainer, Palette.LightOnGreenContainer),
            ColorPair(scheme.secondaryContainer, scheme.onSecondaryContainer),
            ColorPair(Palette.LightOrangeContainer, Palette.LightOnOrangeContainer),
        ),
        cardGradientStart = scheme.primary,
        cardGradientEnd = Palette.CardGradientEnd,
        onCard = Palette.OnCard,
        qrBackground = Palette.QrBackground,
        scannerBackground = Palette.ScannerBackground,
    )

internal fun darkExtendedColors(scheme: ColorScheme) =
    ExtendedColors(
        success = Palette.DarkSuccess,
        warning = Palette.DarkWarning,
        avatars = listOf(
            ColorPair(scheme.primaryContainer, scheme.onPrimaryContainer),
            ColorPair(scheme.tertiaryContainer, scheme.onTertiaryContainer),
            ColorPair(Palette.DarkGreenContainer, Palette.DarkOnGreenContainer),
            ColorPair(scheme.secondaryContainer, scheme.onSecondaryContainer),
            ColorPair(Palette.DarkOrangeContainer, Palette.DarkOnOrangeContainer),
        ),
        cardGradientStart = scheme.primaryContainer,
        cardGradientEnd = Palette.CardGradientEnd,
        onCard = Palette.OnCard,
        qrBackground = Palette.QrBackground,
        scannerBackground = Palette.ScannerBackground,
    )

val LocalExtendedColors = staticCompositionLocalOf { lightExtendedColors(LightColors) }
