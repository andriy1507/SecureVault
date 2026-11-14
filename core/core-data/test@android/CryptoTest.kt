package dev.spaceapps.secvault.core.data.crypto

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CryptoTest {

    private val bip39 by lazy { Bip39(loadWordlist()) }

    /** Official BIP39 test vectors (trezor/python-mnemonic vectors.json), 256-bit entropy. */
    @Test
    fun bip39MatchesOfficialVectors() = runTest {
        val vectors = mapOf(
            "00".repeat(32) to "abandon ".repeat(23) + "art",
            "7f".repeat(32) to "legal winner thank year wave sausage worth useful ".repeat(2) +
                "legal winner thank year wave sausage worth title",
            "80".repeat(32) to "letter advice cage absurd amount doctor acoustic avoid ".repeat(2) +
                "letter advice cage absurd amount doctor acoustic bless",
            "ff".repeat(32) to "zoo ".repeat(23) + "vote",
        )
        for ((hex, phrase) in vectors) {
            val entropy = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val words = bip39.entropyToWords(entropy)
            assertEquals(phrase, words.joinToString(" "))
            assertContentEquals(entropy, bip39.wordsToEntropy(words))
        }
    }

    @Test
    fun bip39RejectsBadChecksum() = runTest {
        val words = ("abandon ".repeat(23) + "abandon").trim().split(" ")
        assertFailsWith<IllegalArgumentException> { bip39.wordsToEntropy(words) }
    }

    @Test
    fun encryptDecryptRoundTripAndTamperDetection() = runTest {
        val key = Crypto.newSymmetricKey()
        val blob = Crypto.encrypt(key, "secret".encodeToByteArray(), ad = "item:1")
        assertEquals("secret", Crypto.decrypt(key, blob, ad = "item:1").decodeToString())
        // Wrong associated data must fail (item swap protection)
        assertFailsWith<Exception> { Crypto.decrypt(key, blob, ad = "item:2") }
        // Flipped bit must fail
        val tampered = blob.copyOf().also { it[it.size - 1] = (it.last().toInt() xor 1).toByte() }
        assertFailsWith<Exception> { Crypto.decrypt(key, tampered, ad = "item:1") }
    }

    @Test
    fun keyBundleSerializationRoundTrip() = runTest {
        val bundle = KeyBundle(KeyPairSet.generate(), KeyPairSet.generate(), Crypto.newSymmetricKey())
        val restored = KeyBundle.deserialize(bundle.serialize())
        assertContentEquals(bundle.identity.signPrivate, restored.identity.signPrivate)
        assertContentEquals(bundle.device.agreePublic, restored.device.agreePublic)
        assertContentEquals(bundle.vaultKey, restored.vaultKey)
    }

    @Test
    fun recoveryRestoresIdentityAndVaultKeyWithFreshDeviceKey() = runTest {
        val account = KeyHierarchy.createAccount(bip39)
        assertEquals(24, account.recoveryWords.size)
        assertTrue(Regex("^[0-9A-F]{4}(-[0-9A-F]{4}){3}$").matches(account.accountId), account.accountId)

        val restored = KeyHierarchy.restoreFromRecovery(account.recoveryWords, account.recoveryBlob, account.accountId, bip39)
        assertContentEquals(account.bundle.vaultKey, restored.vaultKey)
        assertContentEquals(account.bundle.identity.signPrivate, restored.identity.signPrivate)
        assertTrue(!account.bundle.device.signPublic.contentEquals(restored.device.signPublic))

        // Wrong account ID (associated data) must fail
        assertFailsWith<Exception> {
            KeyHierarchy.restoreFromRecovery(account.recoveryWords, account.recoveryBlob, "AAAA-AAAA-AAAA-AAAA", bip39)
        }
    }

    @Test
    fun itemKeyWrapIsBoundToItemId() = runTest {
        val vaultKey = Crypto.newSymmetricKey()
        val (itemKey, wrapped) = KeyHierarchy.newItemKey(vaultKey, "id-1")
        assertContentEquals(itemKey, KeyHierarchy.unwrapItemKey(vaultKey, wrapped, "id-1"))
        assertFailsWith<Exception> { KeyHierarchy.unwrapItemKey(vaultKey, wrapped, "id-2") }
        assertNotEquals(Base32.encode(byteArrayOf(0, 0, 0, 0, 0)), Base32.encode(byteArrayOf(1, 0, 0, 0, 0)))
    }

    private fun loadWordlist(): List<String> {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val f = File(dir, "shared/composeResources/files/bip39_english.txt")
            if (f.exists()) return f.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            dir = dir.parentFile
        }
        error("Wordlist not found")
    }
}
