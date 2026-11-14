package dev.spaceapps.secvault.feature.autofill.domain

import dev.spaceapps.secvault.feature.vault.domain.model.ItemDraft
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.model.toDraft
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultItemRepository
import dev.spaceapps.secvault.feature.vault.domain.usecase.SaveVaultItemUseCase

/** Logins linked to the target, via "Autofill for" entries or the Website field. Needs the vault unlocked. */
class FindAutofillMatchesUseCase(private val repository: VaultItemRepository) {
    suspend operator fun invoke(target: AutofillTarget): List<VaultItem> =
        repository.getAll()
            .filter { it.type == ItemType.Login && it.password.isNotEmpty() }
            .filter { item -> (item.uris + item.url).any { it.isNotBlank() && UriMatcher.matches(target, it) } }
            .sortedWith(compareByDescending<VaultItem> { it.favorite }.thenByDescending { it.updatedAt })
}

/**
 * "Save to SecureVault?" from another app: updates the login with the same target and username,
 * or creates a new one. Needs the vault unlocked.
 */
class SaveAutofillCredentialUseCase(
    private val findMatches: FindAutofillMatchesUseCase,
    private val saveItem: SaveVaultItemUseCase,
) {
    sealed interface Result {
        data class Created(val title: String) : Result
        data class Updated(val title: String) : Result
        data object NothingToSave : Result
    }

    suspend operator fun invoke(target: AutofillTarget, username: String, password: String): Result {
        if (password.isEmpty()) return Result.NothingToSave
        val existing = findMatches(target).firstOrNull { it.username.equals(username.trim(), ignoreCase = true) }
        if (existing != null && existing.password == password) return Result.NothingToSave

        val item = existing
        val title = item?.title ?: target.title
        val draft = (item?.toDraft() ?: ItemDraft(type = ItemType.Login)).copy(
            title = title,
            username = item?.username ?: username,
            password = password,
            url = item?.url ?: if (target is AutofillTarget.Website) target.uri else "",
            uris = (item?.uris.orEmpty() + target.uri).distinct(),
        )
        saveItem(existing = item, draft = draft)
        return if (item != null) Result.Updated(title) else Result.Created(title)
    }
}
