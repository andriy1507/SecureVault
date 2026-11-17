package dev.spaceapps.secvault.core.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

const val VAULT_DATASTORE_FILE = "vault_keys.preferences_pb"
const val SETTINGS_DATASTORE_FILE = "settings.preferences_pb"

/** Koin qualifier of the app settings DataStore (the unqualified one holds the vault keys). */
const val SETTINGS_DATASTORE = "settings_datastore"

fun createVaultDataStore(absolutePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { absolutePath.toPath() })

/** Persists only ciphertext and public data. */
@OptIn(ExperimentalEncodingApi::class)
class VaultKeyStorage(
    private val dataStore: DataStore<Preferences>,
) {
    class Stored(
        val accountId: String?,
        val sealedBundle: ByteArray?,
        val recoveryBlob: ByteArray?,
        val recoveryConfirmed: Boolean,
        /** Encrypted recovery entropy of an unconfirmed vault; removed on confirmation. */
        val pendingRecovery: ByteArray? = null,
        /** Epoch millis when [pendingRecovery] was saved. */
        val pendingCreatedAt: Long? = null,
    )

    suspend fun read(): Stored {
        val prefs = dataStore.data.first()
        return Stored(
            accountId = prefs[ACCOUNT_ID],
            sealedBundle = prefs[SEALED_BUNDLE]?.let(Base64::decode),
            recoveryBlob = prefs[RECOVERY_BLOB]?.let(Base64::decode),
            recoveryConfirmed = prefs[RECOVERY_CONFIRMED] ?: false,
            pendingRecovery = prefs[PENDING_RECOVERY]?.let(Base64::decode),
            pendingCreatedAt = prefs[PENDING_CREATED_AT],
        )
    }

    suspend fun saveNewVault(
        accountId: String,
        sealedBundle: ByteArray,
        recoveryBlob: ByteArray,
        pendingRecovery: ByteArray? = null,
        pendingCreatedAt: Long? = null,
    ) {
        dataStore.edit {
            it.clear()
            it[ACCOUNT_ID] = accountId
            it[SEALED_BUNDLE] = Base64.encode(sealedBundle)
            it[RECOVERY_BLOB] = Base64.encode(recoveryBlob)
            it[RECOVERY_CONFIRMED] = false
            if (pendingRecovery != null && pendingCreatedAt != null) {
                it[PENDING_RECOVERY] = Base64.encode(pendingRecovery)
                it[PENDING_CREATED_AT] = pendingCreatedAt
            }
        }
    }

    suspend fun setRecoveryConfirmed() {
        dataStore.edit {
            it[RECOVERY_CONFIRMED] = true
            it.remove(PENDING_RECOVERY)
            it.remove(PENDING_CREATED_AT)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ACCOUNT_ID = stringPreferencesKey("account_id")
        val SEALED_BUNDLE = stringPreferencesKey("sealed_bundle")
        val RECOVERY_BLOB = stringPreferencesKey("recovery_blob")
        val RECOVERY_CONFIRMED = booleanPreferencesKey("recovery_confirmed")
        val PENDING_RECOVERY = stringPreferencesKey("pending_recovery")
        val PENDING_CREATED_AT = longPreferencesKey("pending_created_at")
    }
}
