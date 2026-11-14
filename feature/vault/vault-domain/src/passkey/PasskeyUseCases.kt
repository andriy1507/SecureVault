package dev.spaceapps.secvault.feature.vault.domain.passkey

import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.Passkey
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.repository.PasskeyCrypto
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultItemRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Size of a new credential id; 16 random bytes like most platform authenticators. */
private const val CREDENTIAL_ID_SIZE = 16

/** The site already has one of the listed passkeys in this vault (WebAuthn excludeCredentials). */
class PasskeyAlreadyExistsException : Exception("A passkey for this account already exists")

/** A website asked to create a passkey (parsed from the platform request). */
data class PasskeyCreationRequest(
    val rpId: String,
    val rpName: String,
    val userHandle: ByteArray,
    val userName: String,
    val userDisplayName: String,
    /** Credential ids the site already knows; creating one of these again must fail. */
    val excludeCredentialIds: List<ByteArray> = emptyList(),
) {
    override fun toString() = "PasskeyCreationRequest(rpId=$rpId)"
}

/** What goes back to the website after registration. */
class CreatedPasskey(
    val credentialId: ByteArray,
    val authenticatorData: ByteArray,
    val attestationObject: ByteArray,
    val publicKeySpki: ByteArray,
    /** Login item the passkey was saved to. */
    val itemId: String,
)

/** A passkey offered at sign-in, with the login it belongs to. */
data class PasskeyMatch(
    val item: VaultItem,
    val passkey: Passkey,
)

/** What goes back to the website after sign-in. */
class PasskeyAssertion(
    val credentialId: ByteArray,
    val authenticatorData: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray,
)

/**
 * Creates a passkey and saves it to the vault. It's attached to the login for that site with the same
 * username (or a login without username), so password, 2FA and passkey stay together; otherwise
 * a new login is created. A passkey for the same account on that site is replaced.
 */
class CreatePasskeyUseCase(
    private val repository: VaultItemRepository,
    private val crypto: PasskeyCrypto,
) {
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend operator fun invoke(request: PasskeyCreationRequest): CreatedPasskey {
        val items = repository.getAll()
        val excluded = request.excludeCredentialIds.map(Base64Url::encode).toSet()
        if (items.any { item -> item.passkeys.any { it.rpId == request.rpId && it.credentialId in excluded } }) {
            throw PasskeyAlreadyExistsException()
        }

        val keyPair = crypto.generateKeyPair()
        val credentialId = crypto.randomBytes(CREDENTIAL_ID_SIZE)
        val now = Clock.System.now().toEpochMilliseconds()
        val userHandle = Base64Url.encode(request.userHandle)
        val passkey = Passkey(
            credentialId = Base64Url.encode(credentialId),
            rpId = request.rpId,
            rpName = request.rpName,
            userHandle = userHandle,
            userName = request.userName,
            userDisplayName = request.userDisplayName,
            privateKey = Base64Url.encode(keyPair.privateKey),
            createdAt = now,
        )

        val login = items.firstOrNull { item ->
            item.type == ItemType.Login && item.matchesRp(request.rpId) &&
                (item.username.isBlank() || item.username.equals(request.userName, ignoreCase = true))
        }
        val item = if (login != null) {
            login.copy(
                username = login.username.ifBlank { request.userName },
                passkeys = login.passkeys.filterNot {
                    it.rpId == request.rpId && it.userHandle == userHandle
                } + passkey,
                updatedAt = now,
            )
        } else {
            val site = "https://${request.rpId}"
            VaultItem(
                id = Uuid.random().toString(),
                type = ItemType.Login,
                title = request.rpName.ifBlank { request.rpId },
                username = request.userName,
                url = site,
                uris = listOf(site),
                createdAt = now,
                updatedAt = now,
                passkeys = listOf(passkey),
            )
        }
        repository.save(item)

        val authenticatorData = WebAuthn.authenticatorData(
            rpIdHash = crypto.sha256(request.rpId.encodeToByteArray()),
            flags = WebAuthn.STANDARD_FLAGS or WebAuthn.FLAG_AT,
            credentialId = credentialId,
            coseKey = WebAuthn.coseKey(keyPair.publicKeyRaw),
        )
        return CreatedPasskey(
            credentialId = credentialId,
            authenticatorData = authenticatorData,
            attestationObject = WebAuthn.noneAttestation(authenticatorData),
            publicKeySpki = keyPair.publicKeySpki,
            itemId = item.id,
        )
    }
}

/** Passkeys usable on [rpId]; when the site lists [allowedCredentialIds], only those. */
class FindPasskeysUseCase(
    private val repository: VaultItemRepository,
) {
    suspend operator fun invoke(
        rpId: String,
        allowedCredentialIds: List<ByteArray> = emptyList(),
    ): List<PasskeyMatch> {
        val allowed = allowedCredentialIds.map(Base64Url::encode).toSet()
        return repository
            .getAll()
            .flatMap { item -> item.passkeys.map { PasskeyMatch(item, it) } }
            .filter { it.passkey.rpId == rpId && (allowed.isEmpty() || it.passkey.credentialId in allowed) }
            .sortedByDescending { it.passkey.createdAt }
    }
}

/** Signs a sign-in challenge: authenticatorData || clientDataHash, with the passkey's private key. */
class AssertPasskeyUseCase(
    private val repository: VaultItemRepository,
    private val crypto: PasskeyCrypto,
) {
    /** Null when the passkey is gone (deleted since the list was shown) or belongs to another site than [rpId]. */
    suspend operator fun invoke(
        itemId: String,
        credentialId: String,
        rpId: String,
        clientDataHash: ByteArray,
    ): PasskeyAssertion? {
        val passkey = repository
            .get(itemId)
            ?.takeIf { !it.isTrashed }
            ?.passkeys
            ?.firstOrNull { it.credentialId == credentialId && it.rpId == rpId } ?: return null
        val authenticatorData = WebAuthn.authenticatorData(
            rpIdHash = crypto.sha256(passkey.rpId.encodeToByteArray()),
            flags = WebAuthn.STANDARD_FLAGS,
        )
        val signature = crypto.sign(Base64Url.decode(passkey.privateKey), authenticatorData + clientDataHash)
        return PasskeyAssertion(
            credentialId = Base64Url.decode(passkey.credentialId),
            authenticatorData = authenticatorData,
            signature = signature,
            userHandle = Base64Url.decode(passkey.userHandle),
        )
    }
}

/** Removes one passkey from a login (the site keeps the public key, so it may still list it). */
class DeletePasskeyUseCase(
    private val repository: VaultItemRepository,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        itemId: String,
        credentialId: String,
    ) {
        val item = repository.get(itemId) ?: return
        repository.save(
            item.copy(
                passkeys = item.passkeys.filterNot { it.credentialId == credentialId },
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }
}

private fun VaultItem.matchesRp(rpId: String): Boolean = (uris + url).any { it.isNotBlank() && matchesRpId(it, rpId) }
