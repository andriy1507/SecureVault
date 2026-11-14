package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.spaceapps.secvault.feature.vault.domain.model.PaymentCard

private const val GROUP_SEPARATOR = " "

/**
 * Shows card digits in the brand's groups ("4417 1234 5678 9010", Amex "3782 822463 10005").
 * The field's value stays digits only; the spaces are drawn, so the cursor skips over them.
 */
internal object CardNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val groups = PaymentCard.groupNumber(digits)
        // Original offsets where a separator follows, e.g. [4, 8, 12] for a 16-digit card.
        val breaks = groups.runningFold(0) { acc, group -> acc + group.length }.drop(1).dropLast(1)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + breaks.count { it < offset }

            override fun transformedToOriginal(offset: Int): Int {
                // Separator k sits at transformed position breaks[k] + k.
                val separatorsBefore = breaks.withIndex().count { (k, b) -> b + k < offset }
                return (offset - separatorsBefore).coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(groups.joinToString(GROUP_SEPARATOR)), mapping)
    }
}

/**
 * Shows "MMYY" digits as "MM/YY". The slash appears as soon as the month is complete,
 * like in banking apps, so the user never types it.
 */
internal object ExpiryTransformation : VisualTransformation {
    private const val MONTH_DIGITS = 2

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = PaymentCard.formatExpiry(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset < MONTH_DIGITS) offset else offset + 1

            override fun transformedToOriginal(offset: Int) =
                (if (offset <= MONTH_DIGITS) offset else offset - 1).coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
