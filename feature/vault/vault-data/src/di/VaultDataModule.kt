package dev.spaceapps.secvault.feature.vault.data.di

import dev.spaceapps.secvault.core.domain.repository.VaultItemRepository
import dev.spaceapps.secvault.feature.vault.data.passkey.PasskeyCryptoImpl
import dev.spaceapps.secvault.feature.vault.data.repository.VaultBackupRepositoryImpl
import dev.spaceapps.secvault.feature.vault.data.repository.VaultItemRepositoryImpl
import dev.spaceapps.secvault.feature.vault.data.totp.TotpGeneratorImpl
import dev.spaceapps.secvault.feature.vault.domain.repository.PasskeyCrypto
import dev.spaceapps.secvault.feature.vault.domain.repository.TotpGenerator
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultBackupRepository
import org.koin.dsl.module

val vaultDataModule = module {
    single<VaultItemRepository> { VaultItemRepositoryImpl(get(), get(), get()) }
    single<VaultBackupRepository> { VaultBackupRepositoryImpl(get()) }
    single<TotpGenerator> { TotpGeneratorImpl() }
    single<PasskeyCrypto> { PasskeyCryptoImpl() }
}
