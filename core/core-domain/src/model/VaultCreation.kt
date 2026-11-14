package dev.spaceapps.secvault.core.domain.model

/**
 * Result of creating a new vault.
 *
 * [recoveryWords] is the only copy of the recovery phrase. Show it once and drop the reference.
 */
class VaultCreation(
    val accountId: String,
    val recoveryWords: List<String>,
)
