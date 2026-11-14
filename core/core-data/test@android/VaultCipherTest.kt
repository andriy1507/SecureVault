package dev.spaceapps.secvault.core.data.cipher

import dev.spaceapps.secvault.core.data.crypto.Crypto
import dev.spaceapps.secvault.core.data.crypto.KeyBundle
import dev.spaceapps.secvault.core.data.crypto.KeyPairSet
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.domain.error.VaultError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class VaultCipherTest {
    private suspend fun openSession() =
        VaultSession().apply {
            open(KeyBundle(KeyPairSet.generate(), KeyPairSet.generate(), Crypto.newSymmetricKey()))
        }

    @Test
    fun roundTripAndRowBinding() =
        runTest {
            val cipher = VaultCipherImpl(openSession())
            val record = cipher.encrypt("a", "hello".encodeToByteArray())
            assertFalse(record.ciphertext.decodeToString(throwOnInvalidSequence = false).contains("hello"))
            assertEquals("hello", cipher.decrypt("a", record).decodeToString())
            // A row copied under another id must not decrypt
            assertFailsWith<Exception> { cipher.decrypt("b", record) }
        }

    @Test
    fun lockedSessionRefuses() =
        runTest {
            val session = openSession()
            val cipher = VaultCipherImpl(session)
            val record = cipher.encrypt("a", "x".encodeToByteArray())
            session.close()
            assertFailsWith<VaultError.Locked> { cipher.decrypt("a", record) }
            assertFailsWith<VaultError.Locked> { cipher.encrypt("a", "x".encodeToByteArray()) }
        }

    @Test
    fun otherVaultKeyCannotDecrypt() =
        runTest {
            val record = VaultCipherImpl(openSession()).encrypt("a", "x".encodeToByteArray())
            assertFailsWith<Exception> { VaultCipherImpl(openSession()).decrypt("a", record) }
        }
}
