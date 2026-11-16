package dev.spaceapps.secvault.feature.vault.domain.usecase

import dev.spaceapps.secvault.core.domain.model.CardValidation
import dev.spaceapps.secvault.core.domain.model.ItemDraft
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.PasswordHistoryEntry
import dev.spaceapps.secvault.core.domain.model.PaymentCard
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.model.VaultItemSummary
import dev.spaceapps.secvault.core.domain.repository.VaultItemRepository
import dev.spaceapps.secvault.feature.vault.domain.model.ItemFilter
import dev.spaceapps.secvault.feature.vault.domain.model.ItemSort
import dev.spaceapps.secvault.feature.vault.domain.model.OtpAuthInfo
import dev.spaceapps.secvault.feature.vault.domain.model.OtpAuthUri
import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode
import dev.spaceapps.secvault.feature.vault.domain.repository.TotpGenerator
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How long deleted items stay in the trash. */
object TrashPolicy {
    const val RETENTION_DAYS = 30
    val retentionMillis: Long = RETENTION_DAYS.days.inWholeMilliseconds
}

/** Items not in the trash. */
class ObserveVaultItemsUseCase(
    private val repository: VaultItemRepository,
) {
    operator fun invoke(): Flow<List<VaultItemSummary>> =
        repository.observeSummaries().map { items -> items.filter { it.trashedAt == null } }

    /** Search, type filter and sort as on the vault screen. */
    fun apply(
        items: List<VaultItemSummary>,
        query: String,
        filter: ItemFilter,
        sort: ItemSort,
    ): List<VaultItemSummary> {
        val q = query.trim()
        return items
            .filter { item ->
                when (filter) {
                    ItemFilter.All -> true
                    ItemFilter.Logins -> item.type == ItemType.Login
                    ItemFilter.Cards -> item.type == ItemType.Card
                    ItemFilter.Notes -> item.type == ItemType.Note
                    ItemFilter.Codes -> item.hasTotp
                }
            }.filter {
                q.isEmpty() || it.title.contains(q, ignoreCase = true) ||
                    it.subtitle.contains(q, ignoreCase = true)
            }.sortedWith(
                when (sort) {
                    ItemSort.Name -> compareBy { it.title.lowercase() }
                    ItemSort.DateAdded -> compareByDescending { it.createdAt }
                    ItemSort.LastModified -> compareByDescending { it.updatedAt }
                },
            )
    }
}

/** Items in the trash, most recently deleted first. */
class ObserveTrashUseCase(
    private val repository: VaultItemRepository,
) {
    operator fun invoke(): Flow<List<VaultItemSummary>> =
        repository.observeSummaries().map { items ->
            items.filter { it.trashedAt != null }.sortedByDescending { it.trashedAt }
        }
}

class GetVaultItemUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(id: String): VaultItem? = repository.get(id)
}

class ToggleFavoriteUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(id: String) {
        val item = repository.get(id) ?: return
        repository.save(item.copy(favorite = !item.favorite))
    }
}

class MoveToTrashUseCase(
    private val repository: VaultItemRepository,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(id: String) {
        val item = repository.get(id) ?: return
        repository.save(item.copy(trashedAt = Clock.System.now().toEpochMilliseconds()))
    }
}

class RestoreFromTrashUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(id: String) {
        val item = repository.get(id) ?: return
        repository.save(item.copy(trashedAt = null))
    }
}

/** Erases an item for good (leaves a tombstone for sync). */
class DeleteVaultItemUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

class EmptyTrashUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(items: List<VaultItemSummary>) {
        items.forEach { repository.delete(it.id) }
    }
}

/** Deletes items that have been in the trash longer than [TrashPolicy.RETENTION_DAYS]. */
class PurgeExpiredTrashUseCase(
    private val repository: VaultItemRepository,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(items: List<VaultItemSummary>) {
        val now = Clock.System.now().toEpochMilliseconds()
        items
            .filter { item -> item.trashedAt?.let { now - it >= TrashPolicy.retentionMillis } == true }
            .forEach { repository.delete(it.id) }
    }
}

/** Days until a trashed item is deleted for good. */
fun daysLeftInTrash(
    trashedAt: Long,
    nowEpochMillis: Long,
): Int {
    val left = TrashPolicy.retentionMillis - (nowEpochMillis - trashedAt)
    return (left / 1.days.inWholeMilliseconds).toInt().coerceIn(0, TrashPolicy.RETENTION_DAYS)
}

class GenerateTotpUseCase(
    private val generator: TotpGenerator,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(secret: String): TotpCode? =
        if (secret.isBlank()) null else generator.generate(secret, Clock.System.now().toEpochMilliseconds())
}

/** What a scanned QR code turned into. */
sealed interface ScannedCode {
    data class Otp(
        val info: OtpAuthInfo,
    ) : ScannedCode

    /** A bare base32 key typed or encoded without the otpauth wrapper. */
    data class BareSecret(
        val secret: String,
    ) : ScannedCode {
        override fun toString() = "BareSecret"
    }

    data object NotAnOtpCode : ScannedCode
}

/** Interprets scanned / pasted text as a 2FA secret, checking that it can actually produce codes. */
class ParseScannedCodeUseCase(
    private val generator: TotpGenerator,
) {
    suspend operator fun invoke(text: String): ScannedCode {
        val trimmed = text.trim()
        if (OtpAuthUri.isOtpAuth(trimmed)) {
            val info = OtpAuthUri.parse(trimmed) ?: return ScannedCode.NotAnOtpCode
            return if (generator.generate(info.secret, 0) != null) ScannedCode.Otp(info) else ScannedCode.NotAnOtpCode
        }
        return if (generator.generate(trimmed, 0) != null) ScannedCode.BareSecret(trimmed) else ScannedCode.NotAnOtpCode
    }
}
