package dev.spaceapps.secvault.core.data.cipher

import dev.spaceapps.secvault.core.data.crypto.Bip39
import dev.spaceapps.secvault.core.data.crypto.Crypto
import dev.spaceapps.secvault.core.data.crypto.KeyHierarchy
import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.data.storage.VaultKeyStorage
import dev.spaceapps.secvault.core.domain.error.VaultError
import kotlin.coroutines.cancellation.CancellationException

/**
 * An encrypted backup. [recoveryBlob] is the account's recovery data (identity + vault key encrypted with
 * the recovery phrase), so only that phrase can open [payload], on any device.
 */
data class SealedBackup(
    val accountId: String,
    val recoveryBlob: ByteArray,
    val payload: ByteArray,
)

/** Encrypts / decrypts backup files. Keys never leave core-data. */
interface BackupCipher {
    /** Needs the vault unlocked ([VaultError.Locked] otherwise). */
    suspend fun seal(plaintext: ByteArray): SealedBackup

    /**
     * Works with or without a vault on this device.
     * Throws [VaultError.InvalidRecoveryPhrase], [VaultError.RecoveryPhraseMismatch] or
     * [VaultError.UnsupportedBackup] (payload damaged).
     */
    suspend fun open(
        backup: SealedBackup,
        words: List<String>,
    ): ByteArray
}

internal class BackupCipherImpl(
    private val session: VaultSession,
    private val storage: VaultKeyStorage,
    private val wordlist: WordlistSource,
) : BackupCipher {
    override suspend fun seal(plaintext: ByteArray): SealedBackup {
        val stored = storage.read()
        val accountId = stored.accountId ?: throw VaultError.NoVault()
        val recoveryBlob = stored.recoveryBlob ?: throw VaultError.NoVault()
        val vaultKey = session.vaultKeyCopy()
        try {
            val backupKey = KeyHierarchy.deriveBackupKey(vaultKey)
            try {
                return SealedBackup(accountId, recoveryBlob, Crypto.encrypt(backupKey, plaintext, payloadAd(accountId)))
            } finally {
                backupKey.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }

    override suspend fun open(
        backup: SealedBackup,
        words: List<String>,
    ): ByteArray {
        val bip39 = Bip39(wordlist.load())
        val vaultKey = KeyHierarchy.recoveryVaultKey(words, backup.recoveryBlob, backup.accountId, bip39)
        try {
            val backupKey = KeyHierarchy.deriveBackupKey(vaultKey)
            try {
                return Crypto.decrypt(backupKey, backup.payload, payloadAd(backup.accountId))
            } catch (e: CancellationException) {
                throw e
            } catch (expected: Exception) {
                // The phrase opened the recovery data, so the payload itself was altered.
                throw VaultError.UnsupportedBackup(expected)
            } finally {
                backupKey.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }

    /** Binds the payload to its account, so a payload can't be paired with another account's recovery data. */
    private fun payloadAd(accountId: String) = "backup:v1:$accountId"
}
