package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.components.AppTextField
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ConfirmDialog
import dev.spaceapps.secvault.core.ui.components.ErrorText
import dev.spaceapps.secvault.core.ui.components.FullScreenProgress
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.StrengthLabel
import dev.spaceapps.secvault.core.ui.components.StrengthLevel
import dev.spaceapps.secvault.core.ui.components.SwitchRow
import dev.spaceapps.secvault.core.ui.components.TonalButton
import dev.spaceapps.secvault.core.ui.components.TopBarButton
import dev.spaceapps.secvault.core.ui.components.VisibilityToggle
import dev.spaceapps.secvault.core.ui.components.secretTransformation
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_delete as coreDelete
import dev.spaceapps.secvault.core.ui.resources.action_save
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.core.ui.theme.Lines
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordStrength
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.PaymentCard
import dev.spaceapps.secvault.feature.vault.domain.usecase.TrashPolicy
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Error
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Field
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.State
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditViewModel
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.card_error_cvv
import dev.spaceapps.secvault.feature.vault.resources.card_error_expiry
import dev.spaceapps.secvault.feature.vault.resources.card_error_number
import dev.spaceapps.secvault.feature.vault.resources.card_expired
import dev.spaceapps.secvault.feature.vault.resources.cd_remove
import dev.spaceapps.secvault.feature.vault.resources.cd_scan
import dev.spaceapps.secvault.feature.vault.resources.edit_card
import dev.spaceapps.secvault.feature.vault.resources.edit_delete
import dev.spaceapps.secvault.feature.vault.resources.edit_delete_subtitle
import dev.spaceapps.secvault.feature.vault.resources.edit_favorite
import dev.spaceapps.secvault.feature.vault.resources.edit_generate
import dev.spaceapps.secvault.feature.vault.resources.edit_login
import dev.spaceapps.secvault.feature.vault.resources.edit_new_card
import dev.spaceapps.secvault.feature.vault.resources.edit_new_login
import dev.spaceapps.secvault.feature.vault.resources.edit_new_note
import dev.spaceapps.secvault.feature.vault.resources.edit_note
import dev.spaceapps.secvault.feature.vault.resources.error_save_failed
import dev.spaceapps.secvault.feature.vault.resources.error_title_required
import dev.spaceapps.secvault.feature.vault.resources.field_card_name
import dev.spaceapps.secvault.feature.vault.resources.field_card_number
import dev.spaceapps.secvault.feature.vault.resources.field_cardholder
import dev.spaceapps.secvault.feature.vault.resources.field_cvv
import dev.spaceapps.secvault.feature.vault.resources.field_expiry
import dev.spaceapps.secvault.feature.vault.resources.field_expiry_hint
import dev.spaceapps.secvault.feature.vault.resources.field_name
import dev.spaceapps.secvault.feature.vault.resources.field_note
import dev.spaceapps.secvault.feature.vault.resources.field_notes
import dev.spaceapps.secvault.feature.vault.resources.field_password
import dev.spaceapps.secvault.feature.vault.resources.field_title
import dev.spaceapps.secvault.feature.vault.resources.field_totp_secret
import dev.spaceapps.secvault.feature.vault.resources.field_username
import dev.spaceapps.secvault.feature.vault.resources.field_website
import dev.spaceapps.secvault.feature.vault.resources.field_website_extra
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_text
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

internal fun PasswordStrength.toLevel(): StrengthLevel =
    when (this) {
        PasswordStrength.Weak -> StrengthLevel.Weak
        PasswordStrength.Fair -> StrengthLevel.Fair
        PasswordStrength.Strong -> StrengthLevel.Strong
        PasswordStrength.VeryStrong -> StrengthLevel.VeryStrong
    }

/** 06 / 21 / 22 · New login, card, note and 32 · Edit item. [itemId] == null: new item. */
@Composable
fun ItemEditScreen(
    itemId: String?,
    newType: ItemType,
    newTitle: String,
    onClose: () -> Unit,
    onSaved: (id: String, isNew: Boolean) -> Unit,
    onTrashed: (id: String, title: String) -> Unit,
    newUsername: String = "",
    newTotpSecret: String = "",
    viewModel: ItemEditViewModel = koinViewModel(key = "edit:${itemId ?: "new"}"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var scanning by remember { mutableStateOf(false) }
    LaunchedEffect(itemId) { viewModel.onIntent(Intent.Load(itemId, newType, newTitle, newUsername, newTotpSecret)) }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is Effect.Saved -> onSaved(effect.id, effect.isNew)
            is Effect.Trashed -> onTrashed(effect.id, effect.title)
            Effect.Close -> onClose()
        }
    }
    if (scanning) {
        ScanCodeScreen(
            onFound = {
                viewModel.onIntent(Intent.TotpScanned(it.issuer, it.account, it.secret))
                scanning = false
            },
            onEnterManually = { scanning = false },
            onClose = { scanning = false },
            viewModelKey = "scan:edit",
        )
    } else {
        ItemEditContent(state, viewModel::onIntent, onScanTotp = { scanning = true })
    }
}

private fun titleOf(
    type: ItemType,
    isNew: Boolean,
): StringResource =
    when (type) {
        ItemType.Login -> if (isNew) Res.string.edit_new_login else Res.string.edit_login
        ItemType.Card -> if (isNew) Res.string.edit_new_card else Res.string.edit_card
        ItemType.Note -> if (isNew) Res.string.edit_new_note else Res.string.edit_note
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemEditContent(
    state: State,
    onIntent: (Intent) -> Unit,
    onScanTotp: () -> Unit = {},
) {
    val d = state.draft
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(titleOf(d.type, state.isNew)),
                navIcon = NavIcon.Close,
                onNavigate = { onIntent(Intent.Close) },
            ) {
                TopBarButton(
                    stringResource(CoreRes.string.action_save),
                    { onIntent(Intent.Save) },
                    enabled = !state.isSaving,
                )
            }
        },
    ) { padding ->
        if (state.isLoading) {
            FullScreenProgress(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Dimens.dp12,
                    bottom = Dimens.dp24,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16),
        ) {
            if (state.isNew) TypeSelector(d.type, onIntent)
            when (d.type) {
                ItemType.Login -> LoginEditor(state, onIntent, onScanTotp)
                ItemType.Card -> CardEditor(state, onIntent)
                ItemType.Note -> NoteEditor(state, onIntent)
            }
            SwitchRow(
                title = stringResource(Res.string.edit_favorite),
                checked = d.favorite,
                onCheckedChange = { onIntent(Intent.ToggleFavorite) },
                leadingIcon = Icons.Outlined.Star,
            )
            val error = when (val e = state.error) {
                null -> null
                Error.TitleRequired -> stringResource(Res.string.error_title_required)
                is Error.SaveFailed -> stringResource(Res.string.error_save_failed, e.reason.orEmpty())
            }
            ErrorText(error)
            if (!state.isNew) {
                ListRow(
                    title = stringResource(Res.string.edit_delete),
                    subtitle = stringResource(Res.string.edit_delete_subtitle, TrashPolicy.RETENTION_DAYS),
                    leadingIcon = Icons.Outlined.Delete,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = { onIntent(Intent.Trash) },
                )
            }
        }
    }

    if (state.confirmTrash) {
        ConfirmDialog(
            icon = Icons.Outlined.Delete,
            title = stringResource(Res.string.trash_dialog_title, d.title),
            text = stringResource(Res.string.trash_dialog_text, TrashPolicy.RETENTION_DAYS),
            confirmText = stringResource(CoreRes.string.coreDelete),
            onConfirm = { onIntent(Intent.ConfirmTrash) },
            onDismiss = { onIntent(Intent.DismissTrash) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(
    selected: ItemType,
    onIntent: (Intent) -> Unit,
) {
    val types = ItemType.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onIntent(Intent.TypeChanged(type)) },
                shape = SegmentedButtonDefaults.itemShape(index, types.size),
            ) { Text(stringResource(type.label)) }
        }
    }
}

@Composable
private fun field(
    field: Field,
    onIntent: (Intent) -> Unit,
): (String) -> Unit = { onIntent(Intent.FieldChanged(field, it)) }

@Composable
private fun LoginEditor(
    state: State,
    onIntent: (Intent) -> Unit,
    onScanTotp: () -> Unit,
) {
    val d = state.draft
    AppTextField(d.title, field(Field.Title, onIntent), stringResource(Res.string.field_name))
    AppTextField(
        d.username,
        field(Field.Username, onIntent),
        stringResource(Res.string.field_username),
        leadingIcon = Icons.Outlined.Person,
        keyboardType = KeyboardType.Email,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        AppTextField(
            d.password,
            field(Field.Password, onIntent),
            stringResource(Res.string.field_password),
            leadingIcon = Icons.Outlined.Key,
            monospace = true,
            keyboardType = KeyboardType.Password,
            visualTransformation = secretTransformation(state.passwordVisible),
            trailing = { VisibilityToggle(state.passwordVisible) { onIntent(Intent.TogglePasswordVisible) } },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
            TonalButton(stringResource(Res.string.edit_generate), {
                onIntent(Intent.GeneratePassword)
            }, icon = Icons.Filled.Password)
            state.strength?.let { StrengthLabel(it.toLevel()) }
        }
    }
    AppTextField(
        d.url,
        field(Field.Url, onIntent),
        stringResource(Res.string.field_website),
        leadingIcon = Icons.Outlined.Language,
        keyboardType = KeyboardType.Uri,
        trailing = {
            IconButton(onClick = {
                onIntent(Intent.AddUri)
            }) { Icon(Icons.Filled.Add, stringResource(Res.string.field_website_extra)) }
        },
    )
    state.extraUris.forEachIndexed { index, uri ->
        AppTextField(
            uri,
            { onIntent(Intent.UriChanged(index, it)) },
            stringResource(Res.string.field_website_extra),
            leadingIcon = Icons.Outlined.Language,
            keyboardType = KeyboardType.Uri,
            trailing = {
                IconButton(onClick = { onIntent(Intent.RemoveUri(index)) }) {
                    Icon(Icons.Outlined.Close, stringResource(Res.string.cd_remove))
                }
            },
        )
    }
    AppTextField(
        d.totpSecret,
        field(Field.Totp, onIntent),
        stringResource(Res.string.field_totp_secret),
        leadingIcon = Icons.Outlined.Schedule,
        monospace = true,
        trailing = {
            IconButton(onClick = onScanTotp) { Icon(Icons.Filled.QrCodeScanner, stringResource(Res.string.cd_scan)) }
        },
    )
    AppTextField(
        d.notes,
        field(Field.Notes, onIntent),
        stringResource(Res.string.field_notes),
        singleLine = false,
        minLines = Lines.NOTES,
    )
}

@Composable
private fun CardEditor(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val d = state.draft
    val check = state.cardValidation
    val brand = PaymentCard.brandOf(d.cardNumber)
    // Mark a field once it looks finished, or every wrong field after a refused save.
    val numberError = check.number != null && (state.showCardErrors || d.cardNumber.length >= brand.typicalLength)
    val expiryComplete = d.cardExpiry.length == PaymentCard.EXPIRY_DIGITS
    val expiryError = check.expiry != null && (state.showCardErrors || expiryComplete)
    val cvvError = check.cvv != null && state.showCardErrors
    CardPreview(d.title, d.cardNumber, d.cardholderName, PaymentCard.formatExpiry(d.cardExpiry))
    AppTextField(d.title, field(Field.Title, onIntent), stringResource(Res.string.field_card_name))
    AppTextField(
        d.cardholderName,
        field(Field.CardholderName, onIntent),
        stringResource(Res.string.field_cardholder),
        leadingIcon = Icons.Outlined.Person,
    )
    FormattedDigitsField(
        d.cardNumber,
        PaymentCard::sanitizeNumber,
        { onIntent(Intent.FieldChanged(Field.CardNumber, it)) },
        stringResource(Res.string.field_card_number),
        leadingIcon = Icons.Outlined.CreditCard,
        monospace = true,
        keyboardType = KeyboardType.Number,
        visualTransformation = CardNumberTransformation,
        isError = numberError,
        supportingText = if (numberError) stringResource(Res.string.card_error_number) else null,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        FormattedDigitsField(
            d.cardExpiry,
            PaymentCard::sanitizeExpiry,
            { onIntent(Intent.FieldChanged(Field.CardExpiry, it)) },
            stringResource(Res.string.field_expiry),
            placeholder = stringResource(Res.string.field_expiry_hint),
            keyboardType = KeyboardType.Number,
            visualTransformation = ExpiryTransformation,
            isError = expiryError,
            supportingText = when {
                expiryError -> stringResource(Res.string.card_error_expiry)
                check.expired -> stringResource(Res.string.card_expired)
                else -> null
            },
            modifier = Modifier.weight(1f),
        )
        AppTextField(
            d.cardCvv,
            { onIntent(Intent.FieldChanged(Field.CardCvv, it)) },
            stringResource(Res.string.field_cvv),
            keyboardType = KeyboardType.NumberPassword,
            isError = cvvError,
            supportingText = if (cvvError) stringResource(Res.string.card_error_cvv, brand.cvvLength) else null,
            visualTransformation = secretTransformation(state.cvvVisible),
            trailing = { VisibilityToggle(state.cvvVisible) { onIntent(Intent.ToggleCvvVisible) } },
            modifier = Modifier.weight(1f),
        )
    }
    AppTextField(
        d.notes,
        field(Field.Notes, onIntent),
        stringResource(Res.string.field_notes),
        singleLine = false,
        minLines = Lines.NOTES,
    )
}

/**
 * Digits-only field whose input is cleaned while typing ([sanitize]: "5" → "05", letters dropped).
 * Keeps its own cursor: when cleaning changed the text, the cursor goes to the end instead of
 * landing in a spot that no longer matches what's on screen.
 */
@Composable
private fun FormattedDigitsField(
    value: String,
    sanitize: (String) -> String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    monospace: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number,
    visualTransformation: VisualTransformation,
    isError: Boolean = false,
    supportingText: String? = null,
    placeholder: String? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // Text set from outside (loading an item): show it with the cursor at the end.
    LaunchedEffect(value) {
        if (value != field.text) field = TextFieldValue(value, TextRange(value.length))
    }
    AppTextField(
        value = field,
        onValueChange = { input ->
            val clean = sanitize(input.text)
            field = if (clean == input.text) input else TextFieldValue(clean, TextRange(clean.length))
            if (clean != value) onValueChange(clean)
        },
        label = label,
        modifier = modifier,
        leadingIcon = leadingIcon,
        monospace = monospace,
        keyboardType = keyboardType,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText,
        placeholder = placeholder,
    )
}

@Composable
private fun NoteEditor(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val d = state.draft
    AppTextField(d.title, field(Field.Title, onIntent), stringResource(Res.string.field_title))
    AppTextField(
        d.notes,
        field(Field.Notes, onIntent),
        stringResource(Res.string.field_note),
        singleLine = false,
        minLines = Lines.NOTE_BODY,
    )
}
