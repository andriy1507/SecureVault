package dev.spaceapps.secvault.feature.vault.data.dto

import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.Passkey
import dev.spaceapps.secvault.feature.vault.domain.model.PasswordHistoryEntry
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import kotlinx.serialization.Serializable

@Serializable
internal class PasswordHistoryDto(val password: String, val changedAt: Long)

@Serializable
internal class PasskeyDto(
    val credentialId: String,
    val rpId: String,
    val rpName: String = "",
    val userHandle: String,
    val userName: String = "",
    val userDisplayName: String = "",
    val privateKey: String,
    val createdAt: Long,
)

private fun PasskeyDto.toDomain() =
    Passkey(credentialId, rpId, rpName, userHandle, userName, userDisplayName, privateKey, createdAt)

private fun Passkey.toDto() =
    PasskeyDto(credentialId, rpId, rpName, userHandle, userName, userDisplayName, privateKey, createdAt)

/**
 * What gets encrypted for one item. Field names are part of the stored format: don't rename.
 * New fields need defaults so items saved by older versions still decode.
 * fieldClocks: last change time per field, for field-level merge once sync exists.
 */
@Serializable
internal class VaultItemPayloadDto(
    val v: Int = 1,
    val type: String,
    val title: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val fieldClocks: Map<String, Long> = emptyMap(),
    val uris: List<String> = emptyList(),
    val totpSecret: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val passwordHistory: List<PasswordHistoryDto> = emptyList(),
    val passkeys: List<PasskeyDto> = emptyList(),
    val trashedAt: Long? = null,
)

internal fun VaultItemPayloadDto.toDomain(id: String) = VaultItem(
    id = id,
    type = runCatching { ItemType.valueOf(type) }.getOrDefault(ItemType.Note),
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    favorite = favorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    uris = uris,
    totpSecret = totpSecret,
    cardholderName = cardholderName,
    cardNumber = cardNumber,
    cardExpiry = cardExpiry,
    cardCvv = cardCvv,
    passwordHistory = passwordHistory.map { PasswordHistoryEntry(it.password, it.changedAt) },
    passkeys = passkeys.map { it.toDomain() },
    trashedAt = trashedAt,
)

private const val LIST_SEPARATOR = "\n"

private fun fieldsOf(
    type: String, title: String, username: String, password: String, url: String, notes: String, favorite: Boolean,
    uris: List<String>, totpSecret: String, cardholderName: String, cardNumber: String, cardExpiry: String,
    cardCvv: String, trashedAt: Long?, passkeyIds: List<String>,
) = mapOf(
    "type" to type, "title" to title, "username" to username, "password" to password, "url" to url,
    "notes" to notes, "favorite" to favorite.toString(), "uris" to uris.joinToString(LIST_SEPARATOR),
    "totpSecret" to totpSecret, "cardholderName" to cardholderName, "cardNumber" to cardNumber,
    "cardExpiry" to cardExpiry, "cardCvv" to cardCvv, "trashedAt" to trashedAt?.toString().orEmpty(),
    "passkeys" to passkeyIds.sorted().joinToString(LIST_SEPARATOR),
)

internal fun VaultItem.toPayload(previous: VaultItemPayloadDto?): VaultItemPayloadDto {
    val fields = fieldsOf(
        type.name, title, username, password, url, notes, favorite, uris, totpSecret,
        cardholderName, cardNumber, cardExpiry, cardCvv, trashedAt, passkeys.map { it.credentialId },
    )
    val clocks = if (previous == null) {
        fields.mapValues { updatedAt }
    } else {
        val oldFields = with(previous) {
            fieldsOf(
                type, title, username, password, url, notes, favorite, uris, totpSecret,
                cardholderName, cardNumber, cardExpiry, cardCvv, trashedAt, passkeys.map { it.credentialId },
            )
        }
        fields.mapValues { (name, value) ->
            if (oldFields[name] == value) previous.fieldClocks[name] ?: updatedAt else updatedAt
        }
    }
    return VaultItemPayloadDto(
        type = type.name, title = title, username = username, password = password, url = url,
        notes = notes, favorite = favorite, createdAt = createdAt, updatedAt = updatedAt, fieldClocks = clocks,
        uris = uris, totpSecret = totpSecret, cardholderName = cardholderName, cardNumber = cardNumber,
        cardExpiry = cardExpiry, cardCvv = cardCvv,
        passwordHistory = passwordHistory.map { PasswordHistoryDto(it.password, it.changedAt) },
        passkeys = passkeys.map { it.toDto() },
        trashedAt = trashedAt,
    )
}
