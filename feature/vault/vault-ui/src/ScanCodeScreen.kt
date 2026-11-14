package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.BottomActions
import dev.spaceapps.secvault.core.ui.components.CenteredContent
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.OutlinedIconTextButton
import dev.spaceapps.secvault.core.ui.components.PrimaryButton
import dev.spaceapps.secvault.core.ui.components.SecondaryButton
import dev.spaceapps.secvault.core.ui.navigation.PlatformBackHandler
import dev.spaceapps.secvault.core.ui.scanner.QrScannerView
import dev.spaceapps.secvault.core.ui.scanner.rememberCameraPermission
import dev.spaceapps.secvault.core.ui.theme.AppTheme
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.presentation.scan.CodePrefill
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.State
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeViewModel
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.cd_torch
import dev.spaceapps.secvault.feature.vault.resources.scan_hint
import dev.spaceapps.secvault.feature.vault.resources.scan_manual
import dev.spaceapps.secvault.feature.vault.resources.scan_not_otp
import dev.spaceapps.secvault.feature.vault.resources.scan_permission_allow
import dev.spaceapps.secvault.feature.vault.resources.scan_permission_body
import dev.spaceapps.secvault.feature.vault.resources.scan_permission_title
import dev.spaceapps.secvault.feature.vault.resources.scan_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Alpha of the hint text over the camera preview. */
private const val HINT_ALPHA = 0.8f

/** 17 · Scan pairing code, reused for 2FA QR codes. */
@Composable
fun ScanCodeScreen(
    onFound: (CodePrefill) -> Unit,
    onEnterManually: () -> Unit,
    onClose: () -> Unit,
    viewModelKey: String = "scan",
    viewModel: ScanCodeViewModel = koinViewModel(key = viewModelKey),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Bumped to recreate the camera view (and its "already delivered" flag) after a non-2FA QR.
    var scanSession by remember { mutableIntStateOf(0) }
    PlatformBackHandler { onClose() }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is Effect.Found -> onFound(effect.prefill)
            Effect.ScanAgain -> scanSession++
            Effect.EnterManually -> onEnterManually()
            Effect.Close -> onClose()
        }
    }
    ScanCodeContent(state, scanSession, viewModel::onIntent)
}

@Composable
internal fun ScanCodeContent(state: State, scanSession: Int, onIntent: (Intent) -> Unit) {
    val permission = rememberCameraPermission()
    val colors = AppTheme.extendedColors
    if (!permission.granted) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            AppTopBar(stringResource(Res.string.scan_title), navIcon = NavIcon.Close, onNavigate = { onIntent(Intent.Close) })
            CenteredContent(Modifier.weight(1f)) {
                HeroMessage(
                    icon = Icons.Filled.QrCodeScanner,
                    title = stringResource(Res.string.scan_permission_title),
                    body = stringResource(Res.string.scan_permission_body),
                )
            }
            BottomActions {
                SecondaryButton(stringResource(Res.string.scan_manual), { onIntent(Intent.EnterManually) }, icon = Icons.Outlined.Keyboard)
                PrimaryButton(stringResource(Res.string.scan_permission_allow), permission::request)
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(colors.scannerBackground)) {
        key(scanSession) {
            QrScannerView(torchOn = state.torchOn, onResult = { onIntent(Intent.Scanned(it)) }, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            AppTopBar(
                title = stringResource(Res.string.scan_title),
                navIcon = NavIcon.Close,
                onNavigate = { onIntent(Intent.Close) },
                containerColor = Color.Transparent,
                contentColor = colors.onCard,
            ) {
                IconButton(onClick = { onIntent(Intent.ToggleTorch) }) {
                    Icon(
                        if (state.torchOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                        stringResource(Res.string.cd_torch),
                        tint = colors.onCard,
                    )
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp24, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(Dimens.scannerFrame)
                        .border(Dimens.scannerFrameBorder, colors.onCard, RoundedCornerShape(Dimens.radiusScannerFrame)),
                )
                Text(
                    stringResource(if (state.notAnOtpCode) Res.string.scan_not_otp else Res.string.scan_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onCard.copy(alpha = HINT_ALPHA),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Dimens.dp48),
                )
            }
            BottomActions {
                OutlinedIconTextButton(
                    stringResource(Res.string.scan_manual),
                    { onIntent(Intent.EnterManually) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Keyboard,
                    contentColor = colors.onCard,
                )
            }
        }
    }
}
