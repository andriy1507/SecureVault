package dev.spaceapps.secvault.core.domain.usecase

import dev.spaceapps.secvault.core.domain.model.CardValidation
import dev.spaceapps.secvault.core.domain.model.ItemDraft
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.PasswordHistoryEntry
import dev.spaceapps.secvault.core.domain.model.PaymentCard
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.repository.VaultItemRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** How many previous passwords a login keeps. */
private const val PASSWORD_HISTORY_LIMIT = 10

class SaveVaultItemUseCase(
    private val repository: VaultItemRepository,
) {
    sealed class Result {
        data class Saved(
            val id: String,
        ) : Result()

        data object TitleRequired : Result()

        /** Card fields that are filled in but wrong; nothing was saved. */
        data class InvalidCard(
            val validation: CardValidation,
        ) : Result()
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    suspend operator fun invoke(
        existing: VaultItem?,
        draft: ItemDraft,
    ): Result {
        if (draft.title.isBlank()) return Result.TitleRequired
        val now = Clock.System.now().toEpochMilliseconds()
        val type = draft.type
        val isLogin = type == ItemType.Login
        val isCard = type == ItemType.Card
        val cardNumber = if (isCard) PaymentCard.sanitizeNumber(draft.cardNumber) else ""
        val cardExpiry = if (isCard) PaymentCard.sanitizeExpiry(draft.cardExpiry) else ""
        val cardCvv = if (isCard) PaymentCard.sanitizeCvv(draft.cardCvv, cardNumber) else ""
        if (isCard) {
            val validation = PaymentCard.validate(cardNumber, cardExpiry, cardCvv, PaymentCard.yearMonthOf(now))
            if (!validation.isValid) return Result.InvalidCard(validation)
        }
        val password = if (isLogin) draft.password else ""
        val history = existing?.passwordHistory.orEmpty().let { old ->
            val previous = existing?.password.orEmpty()
            if (isLogin && previous.isNotEmpty() && previous != password) {
                (listOf(PasswordHistoryEntry(previous, now)) + old).take(PASSWORD_HISTORY_LIMIT)
            } else {
                old
            }
        }
        val item = VaultItem(
            id = existing?.id ?: Uuid.random().toString(),
            type = type,
            title = draft.title.trim(),
            username = if (isLogin) draft.username.trim() else "",
            password = password,
            url = if (isLogin) draft.url.trim() else "",
            notes = draft.notes,
            favorite = draft.favorite,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            uris = if (isLogin) {
                draft.uris
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            } else {
                emptyList()
            },
            totpSecret = if (isLogin) draft.totpSecret.trim() else "",
            cardholderName = if (isCard) draft.cardholderName.trim() else "",
            cardNumber = cardNumber,
            cardExpiry = if (cardExpiry.isEmpty()) "" else PaymentCard.formatExpiry(cardExpiry),
            cardCvv = cardCvv,
            passwordHistory = if (isLogin) history else emptyList(),
            // Passkeys aren't edited in the form; keep the ones the login already has.
            passkeys = if (isLogin) existing?.passkeys.orEmpty() else emptyList(),
            trashedAt = existing?.trashedAt,
        )
        repository.save(item)
        return Result.Saved(item.id)
    }
}
