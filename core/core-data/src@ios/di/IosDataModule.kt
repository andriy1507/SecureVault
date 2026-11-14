package dev.spaceapps.secvault.core.data.di

import androidx.room3.Room
import dev.spaceapps.secvault.core.data.database.AppDatabase
import dev.spaceapps.secvault.core.data.database.VAULT_DB_FILE
import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.data.storage.SETTINGS_DATASTORE
import dev.spaceapps.secvault.core.data.storage.SETTINGS_DATASTORE_FILE
import dev.spaceapps.secvault.core.data.storage.VAULT_DATASTORE_FILE
import dev.spaceapps.secvault.core.data.storage.createVaultDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** [guard] is implemented in Swift (Secure Enclave) and adapted in the iosApp module. */
fun iosDataModule(guard: HardwareKeyGuard): Module =
    module {
        single { guard }
        single { createVaultDataStore("${applicationSupportDir()}/$VAULT_DATASTORE_FILE") }
        single(
            named(SETTINGS_DATASTORE),
        ) { createVaultDataStore("${applicationSupportDir()}/$SETTINGS_DATASTORE_FILE") }
        single { Room.databaseBuilder<AppDatabase>(name = "${applicationSupportDir()}/$VAULT_DB_FILE") }
    }

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDir(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "No Application Support directory" }
}
