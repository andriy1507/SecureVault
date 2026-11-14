package dev.spaceapps.secvault.core.data.cipher

import dev.spaceapps.secvault.core.data.crypto.Crypto
import dev.spaceapps.secvault.core.data.crypto.KeyHierarchy
import dev.spaceapps.secvault.core.data.session.VaultSession

/** An item as stored: its own key wrapped by the vault key, and its content encrypted with that key. */
class EncryptedRecord(
    val wrappedItemKey: ByteArray,
    val ciphertext: ByteArray,
)

/**
 * The only way feature modules touch encryption. Keys never leave core-data.
 * Throws VaultError.Locked when the vault is locked.
 */
interface VaultCipher {
    suspend fun encrypt(
        itemId: String,
        plaintext: ByteArray,
    ): EncryptedRecord

    suspend fun decrypt(
        itemId: String,
        record: EncryptedRecord,
    ): ByteArray
}

internal class VaultCipherImpl(
    private val session: VaultSession,
) : VaultCipher {
    override suspend fun encrypt(
        itemId: String,
        plaintext: ByteArray,
    ): EncryptedRecord {
        val vaultKey = session.vaultKeyCopy()
        try {
            // Fresh item key on every save: old ciphertexts (e.g. on a server later) stay independent.
            val (itemKey, wrapped) = KeyHierarchy.newItemKey(vaultKey, itemId)
            try {
                return EncryptedRecord(wrapped, Crypto.encrypt(itemKey, plaintext, ad = contentAd(itemId)))
            } finally {
                itemKey.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }

    override suspend fun decrypt(
        itemId: String,
        record: EncryptedRecord,
    ): ByteArray {
        val vaultKey = session.vaultKeyCopy()
        try {
            val itemKey = KeyHierarchy.unwrapItemKey(vaultKey, record.wrappedItemKey, itemId)
            try {
                return Crypto.decrypt(itemKey, record.ciphertext, ad = contentAd(itemId))
            } finally {
                itemKey.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }

    /** Binds ciphertext to its row, so rows can't be swapped in the database. */
    private fun contentAd(itemId: String) = "item:$itemId"
}
