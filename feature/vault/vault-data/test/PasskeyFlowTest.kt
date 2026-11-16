package dev.spaceapps.secvault.feature.vault.data.passkey

import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.model.VaultItemSummary
import dev.spaceapps.secvault.core.domain.model.toSummary
import dev.spaceapps.secvault.core.domain.repository.VaultItemRepository
import dev.spaceapps.secvault.feature.vault.domain.passkey.AssertPasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.CreatePasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.FindPasskeysUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.PasskeyAlreadyExistsException
import dev.spaceapps.secvault.feature.vault.domain.passkey.PasskeyCreationRequest
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class PasskeyFlowTest {
    private class FakeRepository : VaultItemRepository {
        val items = MutableStateFlow<Map<String, VaultItem>>(emptyMap())

        override fun observeSummaries(): Flow<List<VaultItemSummary>> =
            items.map { m -> m.values.map { it.toSummary() } }

        override suspend fun get(id: String) = items.value[id]

        override suspend fun getAll() = items.value.values.filter { !it.isTrashed }

        override suspend fun save(item: VaultItem) {
            items.value = items.value + (item.id to item)
        }

        override suspend fun delete(id: String) {
            items.value = items.value - id
        }
    }

    private val repository = FakeRepository()
    private val crypto = PasskeyCryptoImpl()
    private val request = PasskeyCreationRequest(
        rpId = "webauthn.io",
        rpName = "WebAuthn.io",
        userHandle = byteArrayOf(1, 2, 3),
        userName = "andriy",
        userDisplayName = "Andriy",
    )

    @Test
    fun createdPasskeySignsVerifiably() =
        runTest {
            val created = CreatePasskeyUseCase(repository, crypto)(request)
            assertEquals("fmt", created.attestationObject.copyOfRange(2, 5).decodeToString())

            val match = FindPasskeysUseCase(repository)("webauthn.io").single()
            val clientDataHash = crypto.sha256("{\"type\":\"webauthn.get\"}".encodeToByteArray())
            val assertion = assertNotNull(
                AssertPasskeyUseCase(
                    repository,
                    crypto,
                )(match.item.id, match.passkey.credentialId, "webauthn.io", clientDataHash),
            )
            assertContentEquals(byteArrayOf(1, 2, 3), assertion.userHandle)
            // Another site can't use it.
            assertEquals(
                null,
                AssertPasskeyUseCase(
                    repository,
                    crypto,
                )(match.item.id, match.passkey.credentialId, "evil.io", clientDataHash),
            )
            assertContentEquals(created.credentialId, assertion.credentialId)

            val publicKey = CryptographyProvider.Default
                .get(ECDSA)
                .publicKeyDecoder(EC.Curve.P256)
                .decodeFromByteArray(EC.PublicKey.Format.DER, created.publicKeySpki)
            val valid = publicKey
                .signatureVerifier(SHA256, ECDSA.SignatureFormat.DER)
                .tryVerifySignature(assertion.authenticatorData + clientDataHash, assertion.signature)
            assertTrue(valid)
        }

    @Test
    fun attachesToExistingLoginAndHonoursExcludeList() =
        runTest {
            repository.save(
                VaultItem(
                    id = "login",
                    type = ItemType.Login,
                    title = "WebAuthn",
                    username = "andriy",
                    url = "https://webauthn.io",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            val created = CreatePasskeyUseCase(repository, crypto)(request)
            assertEquals("login", created.itemId)
            assertEquals(1, repository.items.value.size)

            assertFailsWith<PasskeyAlreadyExistsException> {
                CreatePasskeyUseCase(
                    repository,
                    crypto,
                )(request.copy(excludeCredentialIds = listOf(created.credentialId)))
            }
        }
}
