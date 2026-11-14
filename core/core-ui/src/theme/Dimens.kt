package dev.spaceapps.secvault.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Every size used by the UI. Screens never write `16.dp` directly: they use `Dimens.dp16`
 * or, where a size has a meaning of its own, the named value below it.
 */
object Dimens {
    val dp0 = 0.dp
    val dp1 = 1.dp
    val dp2 = 2.dp
    val dp3 = 3.dp
    val dp4 = 4.dp
    val dp6 = 6.dp
    val dp8 = 8.dp
    val dp10 = 10.dp
    val dp12 = 12.dp
    val dp16 = 16.dp
    val dp18 = 18.dp
    val dp20 = 20.dp
    val dp24 = 24.dp
    val dp28 = 28.dp
    val dp32 = 32.dp
    val dp36 = 36.dp
    val dp40 = 40.dp
    val dp44 = 44.dp
    val dp48 = 48.dp
    val dp56 = 56.dp
    val dp64 = 64.dp
    val dp72 = 72.dp
    val dp88 = 88.dp
    val dp96 = 96.dp
    val dp120 = 120.dp
    val dp140 = 140.dp
    val dp180 = 180.dp
    val dp260 = 260.dp

    // --- Named sizes from the design -------------------------------------------------------

    /** Horizontal padding of a screen's content. */
    val screenPadding = dp16

    /** Padding of centered "hero" screens (welcome, PIN, empty states). */
    val heroPadding = dp32

    /** Round container of the big icon on hero screens. */
    val heroIconContainer = dp88
    val heroIcon = dp44

    /** Logo on the welcome / splash screen. */
    val logoContainer = dp96
    val logoIcon = dp48
    val splashLogoContainer = dp120
    val splashLogoIcon = dp64

    /** List item avatar (letter or type icon). */
    val avatar = dp40
    val avatarLarge = dp56

    /** Small app mark in sheet headers (autofill). */
    val sheetLogoContainer = dp40
    val sheetLogoIcon = dp24

    /** Minimum height of a vault card row. */
    val vaultCardMinHeight = dp72

    /** Height of the main filled buttons at the bottom of a screen. */
    val bottomButtonHeight = dp48
    val buttonHeight = dp40

    val searchBarHeight = dp56

    /** PIN dots and keypad. */
    val pinDot = dp16
    val pinDotBorder = dp2
    val pinKeyHeight = dp64
    val pinPadHorizontalPadding = dp40
    val biometricButton = dp64
    val biometricIcon = dp32

    /** Step progress bar of the create-account flow. */
    val stepBarHeight = dp4

    /** Strength meter segments. */
    val strengthBarHeight = dp4

    /** Payment card preview in the card editor. */
    val cardPreviewHeight = dp180

    /** TOTP countdown ring. */
    val totpRing = dp32
    val totpRingStroke = dp4

    /** Bottom spacing of lists so the FAB doesn't cover the last item. */
    val fabListPadding = dp96

    val dividerThickness = dp1

    /** QR scanner viewfinder. */
    val scannerFrame = dp260
    val scannerFrameBorder = dp3

    // --- Corner radii ------------------------------------------------------------------------
    val radiusSmall = dp8
    val radiusMedium = dp12
    val radiusLarge = dp16
    val radiusExtraLarge = dp28
    val radiusLogo = dp28
    val radiusSplashLogo = dp36
    val radiusBiometricButton = dp20
    val radiusCardPreview = dp20
    val radiusScannerFrame = dp24
}

/** Number of visible lines for multi-line fields. */
object Lines {
    const val NOTES = 3
    const val RECOVERY_KEY = 4
    const val NOTE_BODY = 10
}

/** Alpha values used for de-emphasised content. */
object Alphas {
    const val DISABLED = 0.38f
    const val HANDLE = 0.4f
}
