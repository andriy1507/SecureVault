package dev.spaceapps.secvault.feature.vault.data.repository

import dev.spaceapps.secvault.core.data.cipher.BackupCipher
import dev.spaceapps.secvault.core.data.cipher.SealedBackup
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.BackupInfo
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.data.dto.BackupEnvelopeDto
import dev.spaceapps.secvault.feature.vault.data.dto.BackupItemDto
import dev.spaceapps.secvault.feature.vault.data.dto.BackupPayloadDto
import dev.spaceapps.secvault.feature.vault.data.dto.toDomain
import dev.spaceapps.secvault.feature.vault.data.dto.toPayload
import dev.spaceapps.secvault.feature.vault.domain.model.DecryptedBackup
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultBackupRepository
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@OptIn(ExperimentalEncodingApi::class)
internal class VaultBackupRepositoryImpl(
    private val cipher: BackupCipher,
) : VaultBackupRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun export(
        items: List<VaultItem>,
        createdAt: Long,
    ): String {
        val payload = BackupPayloadDto(items.map { BackupItemDto(it.id, it.toPayload(previous = null)) })
        val plain = json.encodeToString(BackupPayloadDto.serializer(), payload).encodeToByteArray()
        val sealed = try {
            cipher.seal(plain)
        } finally {
            plain.fill(0)
        }
        val envelope = BackupEnvelopeDto(
            format = BackupEnvelopeDto.FORMAT,
            v = BackupEnvelopeDto.VERSION,
            accountId = sealed.accountId,
            createdAt = createdAt,
            itemCount = items.size,
            recoveryBlob = Base64.encode(sealed.recoveryBlob),
            payload = Base64.encode(sealed.payload),
        )
        return json.encodeToString(BackupEnvelopeDto.serializer(), envelope)
    }

    override fun readInfo(content: String): BackupInfo {
        val envelope = parseEnvelope(content)
        return BackupInfo(envelope.accountId, envelope.createdAt, envelope.itemCount)
    }

    override suspend fun decrypt(
        content: String,
        words: List<String>,
    ): DecryptedBackup {
        val envelope = parseEnvelope(content)
        val sealed = try {
            SealedBackup(envelope.accountId, Base64.decode(envelope.recoveryBlob), Base64.decode(envelope.payload))
        } catch (e: IllegalArgumentException) {
            throw VaultError.UnsupportedBackup(e)
        }
        val plain = cipher.open(sealed, words)
        val payload = try {
            json.decodeFromString(BackupPayloadDto.serializer(), plain.decodeToString())
        } catch (e: SerializationException) {
            throw VaultError.UnsupportedBackup(e)
        } finally {
            plain.fill(0)
        }
        return DecryptedBackup(
            accountId = envelope.accountId,
            recoveryBlob = sealed.recoveryBlob,
            items = payload.items.map { it.item.toDomain(it.id) },
        )
    }

    private fun parseEnvelope(content: String): BackupEnvelopeDto {
        val envelope = try {
            json.decodeFromString(BackupEnvelopeDto.serializer(), content)
        } catch (e: IllegalArgumentException) {
            // SerializationException is one too: not JSON, or not a backup's fields.
            throw VaultError.UnsupportedBackup(e)
        }
        if (envelope.format != BackupEnvelopeDto.FORMAT || envelope.v != BackupEnvelopeDto.VERSION) {
            throw VaultError.UnsupportedBackup()
        }
        return envelope
    }
}
