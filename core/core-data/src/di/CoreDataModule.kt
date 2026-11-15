package dev.spaceapps.secvault.core.data.di

import dev.spaceapps.secvault.core.data.cipher.BackupCipher
import dev.spaceapps.secvault.core.data.cipher.BackupCipherImpl
import dev.spaceapps.secvault.core.data.cipher.VaultCipher
import dev.spaceapps.secvault.core.data.cipher.VaultCipherImpl
import dev.spaceapps.secvault.core.data.database.AppDatabase
import dev.spaceapps.secvault.core.data.database.buildAppDatabase
import dev.spaceapps.secvault.core.data.repository.VaultKeysRepositoryImpl
import dev.spaceapps.secvault.core.data.security.CryptoSecureRandom
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.data.storage.VaultKeyStorage
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import org.koin.dsl.module

/**
 * Needs from platform modules: HardwareKeyGuard, DataStore<Preferences>, RoomDatabase.Builder<AppDatabase>.
 * Needs from the app: WordlistSource.
 */
val coreDataModule = module {
    single { VaultSession() }
    single { VaultKeyStorage(get()) }
    single { buildAppDatabase(get()) }
    single { get<AppDatabase>().vaultItemDao() }
    single<VaultKeysRepository> { VaultKeysRepositoryImpl(get(), get(), get(), get(), get()) }
    single<VaultCipher> { VaultCipherImpl(get()) }
    single<BackupCipher> { BackupCipherImpl(get(), get(), get()) }
    single<SecureRandomSource> { CryptoSecureRandom() }
}
