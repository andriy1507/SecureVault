package dev.spaceapps.secvault.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import dev.spaceapps.secvault.core.ui.files.TextFileFormat
import dev.spaceapps.secvault.core.ui.files.rememberTextFileOpener
import dev.spaceapps.secvault.core.ui.resources.Res
import dev.spaceapps.secvault.core.ui.resources.recovery_phrase_label
import dev.spaceapps.secvault.core.ui.resources.recovery_phrase_open_file
import dev.spaceapps.secvault.core.ui.resources.recovery_phrase_placeholder
import dev.spaceapps.secvault.core.ui.resources.recovery_phrase_word_count
import dev.spaceapps.secvault.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Multi-line input for the 24-word recovery key. Accepts typed or pasted words, or the recovery key file
 * saved during onboarding. [wordCount] is shown under the field.
 */
@Composable
fun RecoveryPhraseField(
    value: String,
    onValueChange: (String) -> Unit,
    wordCount: Int,
    requiredWords: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val opener = rememberTextFileOpener(TextFileFormat.RecoveryKey) { text -> if (text != null) onValueChange(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = stringResource(Res.string.recovery_phrase_label),
            singleLine = false,
            minLines = PHRASE_MIN_LINES,
            monospace = true,
            enabled = enabled,
            isError = isError,
            visualTransformation = VisualTransformation.None,
            placeholder = stringResource(Res.string.recovery_phrase_placeholder),
            supportingText = stringResource(Res.string.recovery_phrase_word_count, wordCount, requiredWords),
        )
        TonalButton(
            text = stringResource(Res.string.recovery_phrase_open_file),
            onClick = opener::open,
            icon = Icons.Outlined.Description,
            enabled = enabled,
        )
    }
}

private const val PHRASE_MIN_LINES = 4
