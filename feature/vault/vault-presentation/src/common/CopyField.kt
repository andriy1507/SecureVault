package dev.spaceapps.secvault.feature.vault.presentation.common

import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem

/** A value that can be copied from an item. The UI turns it into a label ("Password copied"). */
enum class CopyField(val sensitive: Boolean) {
    Username(false),
    Password(true),
    Totp(true),
    Website(false),
    CardNumber(true),
    CardCvv(true),
    CardholderName(false),
    CardExpiry(false),
    Notes(true),
    History(true),
}

/** One-off request to put text on the clipboard. */
data class CopyRequest(val text: String, val field: CopyField) {
    override fun toString() = "CopyRequest($field)" // never log the value
}

/** What the copy button on a vault card copies for each item type. */
fun VaultItem.quickCopy(): CopyRequest? = when (type) {
    ItemType.Login -> password.takeIf { it.isNotEmpty() }?.let { CopyRequest(it, CopyField.Password) }
        ?: username.takeIf { it.isNotEmpty() }?.let { CopyRequest(it, CopyField.Username) }
    ItemType.Card -> cardNumber.takeIf { it.isNotEmpty() }?.let { CopyRequest(it, CopyField.CardNumber) }
    ItemType.Note -> notes.takeIf { it.isNotEmpty() }?.let { CopyRequest(it, CopyField.Notes) }
}

fun VaultItem.valueOf(field: CopyField): String = when (field) {
    CopyField.Username -> username
    CopyField.Password -> password
    CopyField.Website -> url
    CopyField.CardNumber -> cardNumber
    CopyField.CardCvv -> cardCvv
    CopyField.CardholderName -> cardholderName
    CopyField.CardExpiry -> cardExpiry
    CopyField.Notes -> notes
    CopyField.Totp, CopyField.History -> ""
}
