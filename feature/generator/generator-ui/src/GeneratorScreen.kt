package dev.spaceapps.secvault.feature.generator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.clipboard.rememberCopyAction
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.ChevronTrailing
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.FilledIconTextButton
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.RadioOptionDialog
import dev.spaceapps.secvault.core.ui.components.SectionHeader
import dev.spaceapps.secvault.core.ui.components.StrengthLevel
import dev.spaceapps.secvault.core.ui.components.StrengthMeter
import dev.spaceapps.secvault.core.ui.components.SwitchRow
import dev.spaceapps.secvault.core.ui.components.TonalButton
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_copy
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratorMode
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseSeparator
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordStrength
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameType
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.Effect
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.Intent
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.State
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorViewModel
import dev.spaceapps.secvault.feature.generator.resources.Res
import dev.spaceapps.secvault.feature.generator.resources.avoid_ambiguous
import dev.spaceapps.secvault.feature.generator.resources.avoid_ambiguous_desc
import dev.spaceapps.secvault.feature.generator.resources.capitalize
import dev.spaceapps.secvault.feature.generator.resources.cd_decrease
import dev.spaceapps.secvault.feature.generator.resources.cd_history
import dev.spaceapps.secvault.feature.generator.resources.cd_increase
import dev.spaceapps.secvault.feature.generator.resources.copied_label_passphrase
import dev.spaceapps.secvault.feature.generator.resources.copied_label_password
import dev.spaceapps.secvault.feature.generator.resources.copied_label_username
import dev.spaceapps.secvault.feature.generator.resources.generator_title
import dev.spaceapps.secvault.feature.generator.resources.history_clear
import dev.spaceapps.secvault.feature.generator.resources.history_empty
import dev.spaceapps.secvault.feature.generator.resources.history_hours_ago
import dev.spaceapps.secvault.feature.generator.resources.history_just_now
import dev.spaceapps.secvault.feature.generator.resources.history_minutes_ago
import dev.spaceapps.secvault.feature.generator.resources.history_title
import dev.spaceapps.secvault.feature.generator.resources.include_number
import dev.spaceapps.secvault.feature.generator.resources.length
import dev.spaceapps.secvault.feature.generator.resources.length_range
import dev.spaceapps.secvault.feature.generator.resources.lowercase
import dev.spaceapps.secvault.feature.generator.resources.lowercase_desc
import dev.spaceapps.secvault.feature.generator.resources.mode_passphrase
import dev.spaceapps.secvault.feature.generator.resources.mode_password
import dev.spaceapps.secvault.feature.generator.resources.mode_username
import dev.spaceapps.secvault.feature.generator.resources.numbers
import dev.spaceapps.secvault.feature.generator.resources.numbers_desc
import dev.spaceapps.secvault.feature.generator.resources.options
import dev.spaceapps.secvault.feature.generator.resources.regenerate
import dev.spaceapps.secvault.feature.generator.resources.separator
import dev.spaceapps.secvault.feature.generator.resources.separator_comma
import dev.spaceapps.secvault.feature.generator.resources.separator_hyphen
import dev.spaceapps.secvault.feature.generator.resources.separator_period
import dev.spaceapps.secvault.feature.generator.resources.separator_space
import dev.spaceapps.secvault.feature.generator.resources.separator_underscore
import dev.spaceapps.secvault.feature.generator.resources.symbols
import dev.spaceapps.secvault.feature.generator.resources.symbols_desc
import dev.spaceapps.secvault.feature.generator.resources.uppercase
import dev.spaceapps.secvault.feature.generator.resources.uppercase_desc
import dev.spaceapps.secvault.feature.generator.resources.username_chars
import dev.spaceapps.secvault.feature.generator.resources.username_chars_example
import dev.spaceapps.secvault.feature.generator.resources.username_type
import dev.spaceapps.secvault.feature.generator.resources.username_words
import dev.spaceapps.secvault.feature.generator.resources.username_words_example
import dev.spaceapps.secvault.feature.generator.resources.words
import dev.spaceapps.secvault.feature.generator.resources.words_range
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MODES = listOf(
    GeneratorMode.Password to Res.string.mode_password,
    GeneratorMode.Passphrase to Res.string.mode_passphrase,
    GeneratorMode.Username to Res.string.mode_username,
)

private val SEPARATORS: List<Pair<PassphraseSeparator, StringResource>> = listOf(
    PassphraseSeparator.Hyphen to Res.string.separator_hyphen,
    PassphraseSeparator.Space to Res.string.separator_space,
    PassphraseSeparator.Period to Res.string.separator_period,
    PassphraseSeparator.Underscore to Res.string.separator_underscore,
    PassphraseSeparator.Comma to Res.string.separator_comma,
)

private val GeneratorMode.copyLabel: StringResource
    get() = when (this) {
        GeneratorMode.Password -> Res.string.copied_label_password
        GeneratorMode.Passphrase -> Res.string.copied_label_passphrase
        GeneratorMode.Username -> Res.string.copied_label_username
    }

private fun PasswordStrength.toLevel(): StrengthLevel =
    when (this) {
        PasswordStrength.Weak -> StrengthLevel.Weak
        PasswordStrength.Fair -> StrengthLevel.Fair
        PasswordStrength.Strong -> StrengthLevel.Strong
        PasswordStrength.VeryStrong -> StrengthLevel.VeryStrong
    }

/** 02 · Generator, 39 · passphrase, 40 · username, 41 · history sheet. */
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    val scope = rememberCoroutineScope()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is Effect.Copy -> scope.launch {
                copy.copy(
                    effect.text,
                    sensitive = effect.mode != GeneratorMode.Username,
                    label = getString(effect.mode.copyLabel),
                )
            }
        }
    }
    GeneratorContent(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GeneratorContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(stringResource(Res.string.generator_title)) {
                IconButton(onClick = { onIntent(Intent.ShowHistory(true)) }) {
                    Icon(Icons.Filled.History, stringResource(Res.string.cd_history))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MODES.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { onIntent(Intent.ModeChanged(mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, MODES.size),
                    ) { Text(stringResource(label)) }
                }
            }
            ResultBox(state)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                TonalButton(stringResource(Res.string.regenerate), {
                    onIntent(Intent.Regenerate)
                }, Modifier.weight(1f), Icons.Filled.Refresh)
                FilledIconTextButton(
                    stringResource(CoreRes.string.action_copy),
                    { onIntent(Intent.Copy) },
                    Modifier.weight(1f),
                    Icons.Outlined.ContentCopy,
                )
            }
            SectionHeader(stringResource(Res.string.options))
            when (state.mode) {
                GeneratorMode.Password -> PasswordOptionsGroup(state.password, onIntent)
                GeneratorMode.Passphrase -> PassphraseOptionsGroup(state.passphrase, onIntent)
                GeneratorMode.Username -> UsernameOptionsGroup(state.usernameType, onIntent)
            }
        }
    }

    if (state.showHistory) HistorySheet(state.history, onIntent)
    if (state.showSeparatorDialog) {
        RadioOptionDialog(
            title = stringResource(Res.string.separator),
            options = SEPARATORS.map { stringResource(it.second) },
            selectedIndex = SEPARATORS.indexOfFirst { it.first == state.passphrase.separator },
            onConfirm = { onIntent(Intent.SeparatorChosen(SEPARATORS[it].first)) },
            onDismiss = { onIntent(Intent.ShowSeparatorDialog(false)) },
        )
    }
}

@Composable
private fun ResultBox(state: State) {
    Surface(
        shape = RoundedCornerShape(Dimens.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(
                start = Dimens.dp20,
                end = Dimens.dp20,
                top = Dimens.dp28,
                bottom = Dimens.dp20,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.dp20),
        ) {
            Text(
                state.value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
            state.strength?.let { StrengthMeter(it.toLevel()) }
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.dp4)) {
        val colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        FilledTonalIconButton(onClick = {
            onChange(value - 1)
        }, enabled = value > range.first, colors = colors, modifier = Modifier.size(Dimens.dp40)) {
            Icon(Icons.Filled.Remove, stringResource(Res.string.cd_decrease))
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = Dimens.dp40),
        )
        FilledTonalIconButton(onClick = {
            onChange(value + 1)
        }, enabled = value < range.last, colors = colors, modifier = Modifier.size(Dimens.dp40)) {
            Icon(Icons.Filled.Add, stringResource(Res.string.cd_increase))
        }
    }
}

@Composable
private fun PasswordOptionsGroup(
    options: PasswordOptions,
    onIntent: (Intent) -> Unit,
) {
    fun change(o: PasswordOptions) = onIntent(Intent.PasswordOptionsChanged(o))
    val range = PasswordOptions.MIN_LENGTH..PasswordOptions.MAX_LENGTH
    ListGroup {
        ListRow(
            title = stringResource(Res.string.length),
            subtitle = stringResource(Res.string.length_range, range.first, range.last),
            trailing = { Stepper(options.length, range) { change(options.copy(length = it)) } },
        )
        Slider(
            value = options.length.toFloat(),
            onValueChange = { change(options.copy(length = it.roundToInt())) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.padding(horizontal = Dimens.dp16),
        )
        ListDivider()
        // At least one character set stays on.
        val enabledSets = listOf(options.uppercase, options.lowercase, options.digits, options.symbols).count { it }

        fun canTurnOff(current: Boolean) = !current || enabledSets > 1
        SwitchRow(
            stringResource(Res.string.uppercase),
            options.uppercase,
            {
                if (canTurnOff(options.uppercase)) change(options.copy(uppercase = it))
            },
            subtitle = stringResource(Res.string.uppercase_desc),
        )
        SwitchRow(
            stringResource(Res.string.lowercase),
            options.lowercase,
            {
                if (canTurnOff(options.lowercase)) change(options.copy(lowercase = it))
            },
            subtitle = stringResource(Res.string.lowercase_desc),
        )
        SwitchRow(
            stringResource(Res.string.numbers),
            options.digits,
            {
                if (canTurnOff(options.digits)) change(options.copy(digits = it))
            },
            subtitle = stringResource(Res.string.numbers_desc),
        )
        SwitchRow(
            stringResource(Res.string.symbols),
            options.symbols,
            {
                if (canTurnOff(options.symbols)) change(options.copy(symbols = it))
            },
            subtitle = stringResource(Res.string.symbols_desc),
        )
        SwitchRow(
            stringResource(Res.string.avoid_ambiguous),
            options.avoidAmbiguous,
            { change(options.copy(avoidAmbiguous = it)) },
            subtitle = stringResource(Res.string.avoid_ambiguous_desc),
        )
    }
}

@Composable
private fun PassphraseOptionsGroup(
    options: PassphraseOptions,
    onIntent: (Intent) -> Unit,
) {
    fun change(o: PassphraseOptions) = onIntent(Intent.PassphraseOptionsChanged(o))
    val range = PassphraseOptions.MIN_WORDS..PassphraseOptions.MAX_WORDS
    ListGroup {
        ListRow(
            title = stringResource(Res.string.words),
            subtitle = stringResource(Res.string.words_range, range.first, range.last),
            trailing = { Stepper(options.words, range) { change(options.copy(words = it)) } },
        )
        ListDivider()
        ListRow(
            title = stringResource(Res.string.separator),
            subtitle = stringResource(SEPARATORS.first { it.first == options.separator }.second),
            trailing = { ChevronTrailing() },
            onClick = { onIntent(Intent.ShowSeparatorDialog(true)) },
        )
        SwitchRow(stringResource(Res.string.capitalize), options.capitalize, { change(options.copy(capitalize = it)) })
        SwitchRow(stringResource(Res.string.include_number), options.includeNumber, {
            change(options.copy(includeNumber = it))
        })
    }
}

@Composable
private fun UsernameOptionsGroup(
    selected: UsernameType,
    onIntent: (Intent) -> Unit,
) {
    ListGroup {
        Text(
            stringResource(Res.string.username_type),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = Dimens.dp16, top = Dimens.dp12),
        )
        listOf(
            Triple(UsernameType.RandomWords, Res.string.username_words, Res.string.username_words_example),
            Triple(UsernameType.RandomCharacters, Res.string.username_chars, Res.string.username_chars_example),
        ).forEach { (type, title, example) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = type == selected, role = Role.RadioButton, onClick = {
                        onIntent(Intent.UsernameTypeChanged(type))
                    })
                    .heightIn(min = Dimens.dp56)
                    .padding(horizontal = Dimens.dp16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
            ) {
                RadioButton(selected = type == selected, onClick = null)
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(example),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    history: List<GeneratedValue>,
    onIntent: (Intent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(Intent.ShowHistory(false)) }) {
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.dp24, end = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { onIntent(Intent.ClearHistory) },
                ) { Text(stringResource(Res.string.history_clear)) }
            }
        }
        if (history.isEmpty()) {
            Text(
                stringResource(Res.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.dp24, vertical = Dimens.dp16),
            )
        }
        history.forEachIndexed { index, entry ->
            ListRow(
                title = entry.value,
                subtitle = timeAgo(entry.createdAt),
                modifier = Modifier.padding(horizontal = Dimens.dp8),
                trailing = {
                    IconButton(onClick = { onIntent(Intent.CopyHistory(index)) }) {
                        Icon(Icons.Outlined.ContentCopy, stringResource(CoreRes.string.action_copy))
                    }
                },
            )
        }
        Spacer(Modifier.size(Dimens.dp24))
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun timeAgo(epochMillis: Long): String {
    val elapsed = Clock.System.now().toEpochMilliseconds() - epochMillis
    return when {
        elapsed < 1.minutes.inWholeMilliseconds -> stringResource(Res.string.history_just_now)

        elapsed < 1.hours.inWholeMilliseconds -> stringResource(
            Res.string.history_minutes_ago,
            (
                elapsed /
                    1.minutes.inWholeMilliseconds
            ).toInt(),
        )

        else -> stringResource(Res.string.history_hours_ago, (elapsed / 1.hours.inWholeMilliseconds).toInt())
    }
}
