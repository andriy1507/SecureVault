package dev.spaceapps.secvault.core.data.crypto

internal class NewAccount(
    val accountId: String,
    val bundle: KeyBundle,
    val recoveryWords: List<String>,
    /** Identity + vault keys encrypted with the recovery key (for backup / later server). */
    val recoveryBlob: ByteArray,
)

/**
 * Key hierarchy:
 * recovery words -> HKDF -> recovery wrap key -> [identity keys, vault key]
 * vault key -> item keys -> item data (all AES-256-GCM)
 */
internal object KeyHierarchy {
    private const val RECOVERY_INFO = "secvault-recovery-wrap-v1"
    private const val RECOVERY_FORMAT: Byte = 1
    private const val ACCOUNT_ID_BYTES = 8
    private const val ACCOUNT_ID_GROUP = 4
    private const val ACCOUNT_ID_SEPARATOR = "-"
    private const val BYTE_MASK = 0xFF
    private const val HEX_RADIX = 16

    suspend fun createAccount(bip39: Bip39): NewAccount {
        val identity = KeyPairSet.generate()
        val device = KeyPairSet.generate()
        val vaultKey = Crypto.newSymmetricKey()

        val recoveryEntropy = Crypto.randomBytes(32) // 256 bits -> 24 words
        val words = bip39.entropyToWords(recoveryEntropy)
        val accountId = deriveAccountId(identity.signPublic)
        val recoveryBlob = sealForRecovery(recoveryEntropy, identity, vaultKey, accountId)
        recoveryEntropy.fill(0)

        return NewAccount(accountId, KeyBundle(identity, device, vaultKey), words, recoveryBlob)
    }

    /** Restore on a new phone. Device keys are always fresh. */
    suspend fun restoreFromRecovery(
        words: List<String>,
        recoveryBlob: ByteArray,
        accountId: String,
        bip39: Bip39,
    ): KeyBundle {
        val entropy = bip39.wordsToEntropy(words)
        val wrapKey = Crypto.hkdf(entropy, RECOVERY_INFO)
        entropy.fill(0)
        val plain = Crypto.decrypt(wrapKey, recoveryBlob, ad = accountId)
        wrapKey.fill(0)

        val f = Fields.read(RECOVERY_FORMAT, plain)
        plain.fill(0)
        require(f.size == 5) { "Corrupted recovery data" }
        return KeyBundle(KeyPairSet.fromFields(f, 0), KeyPairSet.generate(), f[4])
    }

    // --- per-item keys -------------------------------------------------------

    /** New random key for one item and its wrapped form (stored next to the item). */
    suspend fun newItemKey(
        vaultKey: ByteArray,
        itemId: String,
    ): Pair<ByteArray, ByteArray> {
        val itemKey = Crypto.newSymmetricKey()
        return itemKey to Crypto.encrypt(vaultKey, itemKey, ad = "itemkey:$itemId")
    }

    suspend fun unwrapItemKey(
        vaultKey: ByteArray,
        wrapped: ByteArray,
        itemId: String,
    ): ByteArray = Crypto.decrypt(vaultKey, wrapped, ad = "itemkey:$itemId")

    // --- internals ------------------------------------------------------------

    /**
     * Account ID = first 8 bytes of SHA-256(identity signing public key) as 16 hex digits,
     * in groups of 4: "7F3A-91C2-E04B-D58E".
     */
    private suspend fun deriveAccountId(identitySignPublic: ByteArray): String =
        Crypto
            .sha256(identitySignPublic)
            .copyOfRange(0, ACCOUNT_ID_BYTES)
            .joinToString("") { (it.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0') }
            .uppercase()
            .chunked(ACCOUNT_ID_GROUP)
            .joinToString(ACCOUNT_ID_SEPARATOR)

    private suspend fun sealForRecovery(
        entropy: ByteArray,
        identity: KeyPairSet,
        vaultKey: ByteArray,
        accountId: String,
    ): ByteArray {
        val wrapKey = Crypto.hkdf(entropy, RECOVERY_INFO)
        val plain = Fields.write(RECOVERY_FORMAT, identity.fields() + listOf(vaultKey))
        val blob = Crypto.encrypt(wrapKey, plain, ad = accountId)
        plain.fill(0)
        wrapKey.fill(0)
        return blob
    }
}
