package dev.spaceapps.secvault.core.data.di

import android.content.Context
import androidx.room3.Room
import dev.spaceapps.secvault.core.data.database.AppDatabase
import dev.spaceapps.secvault.core.data.database.VAULT_DB_FILE
import dev.spaceapps.secvault.core.data.security.AndroidHardwareKeyGuard
import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.data.storage.SETTINGS_DATASTORE
import dev.spaceapps.secvault.core.data.storage.SETTINGS_DATASTORE_FILE
import dev.spaceapps.secvault.core.data.storage.VAULT_DATASTORE_FILE
import dev.spaceapps.secvault.core.data.storage.createVaultDataStore
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun androidDataModule(context: Context): Module =
    module {
        val appContext = context.applicationContext
        single<HardwareKeyGuard> { AndroidHardwareKeyGuard(appContext) }
        single { createVaultDataStore(appContext.filesDir.resolve(VAULT_DATASTORE_FILE).absolutePath) }
        single(named(SETTINGS_DATASTORE)) {
            createVaultDataStore(appContext.filesDir.resolve(SETTINGS_DATASTORE_FILE).absolutePath)
        }
        single {
            Room.databaseBuilder<AppDatabase>(
                context = appContext,
                name = appContext.getDatabasePath(VAULT_DB_FILE).absolutePath,
            )
        }
    }
