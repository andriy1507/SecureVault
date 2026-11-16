package dev.spaceapps.secvault.core.domain.model

enum class ItemType { Login, Card, Note }

/** A previous password of a login, kept when the password is changed. */
data class PasswordHistoryEntry(
    val password: String,
    val changedAt: Long,
) {
    override fun toString() = "PasswordHistoryEntry(changedAt=$changedAt)"
}

/** A decrypted item. Exists only in memory while the vault is unlocked. */
data class VaultItem(
    val id: String,
    val type: ItemType,
    val title: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    /** Where this login is used: "https://example.com" or "androidapp://com.example". Used by autofill. */
    val uris: List<String> = emptyList(),
    /** Base32 secret or an otpauth:// URI. */
    val totpSecret: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    /** "MM/YY". */
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
    /** Passkeys for this login's website, created through the system passkey prompt. */
    val passkeys: List<Passkey> = emptyList(),
    /** Set while the item is in the trash. */
    val trashedAt: Long? = null,
) {
    val isTrashed: Boolean get() = trashedAt != null

    /** Never print secrets to logs or crash reports. */
    override fun toString(): String = "VaultItem(id=$id, type=$type)"
}

/** What the lists show: no secrets. */
data class VaultItemSummary(
    val id: String,
    val type: ItemType,
    val title: String,
    /** Login: username (or website); card: expiry; note: empty. */
    val subtitle: String,
    /** Last digits of a card number, for "•••• 4417". */
    val cardLastDigits: String,
    /** Has a 2FA (TOTP) secret: shown in the Codes list. */
    val hasTotp: Boolean,
    val favorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val trashedAt: Long?,
)

/** Digits of a card number shown when masked. */
const val CARD_VISIBLE_DIGITS = 4

fun VaultItem.toSummary() =
    VaultItemSummary(
        id = id,
        type = type,
        title = title,
        subtitle = when (type) {
            ItemType.Login -> username.ifBlank { url }
            ItemType.Card -> cardExpiry
            ItemType.Note -> ""
        },
        cardLastDigits = cardNumber.filter { it.isDigit() }.takeLast(CARD_VISIBLE_DIGITS),
        hasTotp = totpSecret.isNotBlank(),
        favorite = favorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
        trashedAt = trashedAt,
    )

/** What the user typed in the editor. */
data class ItemDraft(
    val type: ItemType = ItemType.Login,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val uris: List<String> = emptyList(),
    val totpSecret: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
) {
    override fun toString() = "ItemDraft(type=$type)" // no secrets in logs
}

fun VaultItem.toDraft() =
    ItemDraft(
        type = type,
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        favorite = favorite,
        uris = uris,
        totpSecret = totpSecret,
        cardholderName = cardholderName,
        cardNumber = cardNumber,
        cardExpiry = cardExpiry,
        cardCvv = cardCvv,
    )
