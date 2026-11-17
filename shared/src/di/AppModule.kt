package dev.spaceapps.secvault.di

import dev.spaceapps.secvault.ComposeResourceWordlist
import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.core.data.di.coreDataModule
import dev.spaceapps.secvault.core.domain.repository.BackupRestorer
import dev.spaceapps.secvault.core.domain.usecase.EstimateStrengthUseCase
import dev.spaceapps.secvault.core.domain.usecase.GeneratePasswordUseCase
import dev.spaceapps.secvault.core.domain.usecase.SaveVaultItemUseCase
import dev.spaceapps.secvault.feature.autofill.domain.FindAutofillMatchesUseCase
import dev.spaceapps.secvault.feature.autofill.domain.SaveAutofillCredentialUseCase
import dev.spaceapps.secvault.feature.generator.data.generatorDataModule
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorWordsSource
import dev.spaceapps.secvault.feature.generator.domain.usecase.AddToGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.ClearGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.GeneratePassphraseUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.GenerateUsernameUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.ObserveGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorViewModel
import dev.spaceapps.secvault.feature.lock.domain.usecase.GetAccountIdUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.LockVaultUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.ObserveLockStateUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.RefreshLockStateUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.UnlockVaultUseCase
import dev.spaceapps.secvault.feature.lock.presentation.LockViewModel
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.BuildConfirmationChoicesUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CheckDeviceSecurityUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.HasPendingVaultUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.ResumeVaultUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CreateVaultUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.PickConfirmationWordsUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.ReadRestoreInfoUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.RestoreAccountUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.VerifyConfirmationWordsUseCase
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingViewModel
import dev.spaceapps.secvault.feature.settings.data.settingsDataModule
import dev.spaceapps.secvault.feature.settings.domain.DeleteAccountUseCase
import dev.spaceapps.secvault.feature.settings.domain.GetAccountIdForSettingsUseCase
import dev.spaceapps.secvault.feature.settings.domain.LockNowUseCase
import dev.spaceapps.secvault.feature.settings.domain.ObserveSettingsUseCase
import dev.spaceapps.secvault.feature.settings.domain.UpdateSettingsUseCase
import dev.spaceapps.secvault.feature.settings.presentation.SettingsViewModel
import dev.spaceapps.secvault.feature.vault.data.di.vaultDataModule
import dev.spaceapps.secvault.feature.vault.domain.passkey.AssertPasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.CreatePasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.DeletePasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.FindPasskeysUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.DeleteVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.EmptyTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ExportVaultUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GenerateTotpUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultAccountIdUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ImportBackupUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.MoveToTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ObserveTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ObserveVaultItemsUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ParseScannedCodeUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.PurgeExpiredTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ReadBackupInfoUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromBackupUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ToggleFavoriteUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.VaultBackupRestorer
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupViewModel
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailViewModel
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditViewModel
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListViewModel
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeViewModel
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val onboardingModule = module {
    factoryOf(::CheckDeviceSecurityUseCase)
    factoryOf(::CreateVaultUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::HasPendingVaultUseCase)
    factoryOf(::ResumeVaultUseCase)
    factoryOf(::PickConfirmationWordsUseCase)
    factoryOf(::BuildConfirmationChoicesUseCase)
    factoryOf(::VerifyConfirmationWordsUseCase)
    factoryOf(::ReadRestoreInfoUseCase)
    factoryOf(::RestoreAccountUseCase)
    viewModelOf(::OnboardingViewModel)
}

private val lockModule = module {
    factoryOf(::ObserveLockStateUseCase)
    factoryOf(::RefreshLockStateUseCase)
    factoryOf(::UnlockVaultUseCase)
    factoryOf(::LockVaultUseCase)
    factoryOf(::GetAccountIdUseCase)
    viewModelOf(::LockViewModel)
}

private val vaultModule = module {
    includes(vaultDataModule)
    factoryOf(::ObserveVaultItemsUseCase)
    factoryOf(::ObserveTrashUseCase)
    factoryOf(::GetVaultItemUseCase)
    factoryOf(::SaveVaultItemUseCase)
    factoryOf(::CreatePasskeyUseCase)
    factoryOf(::FindPasskeysUseCase)
    factoryOf(::AssertPasskeyUseCase)
    factoryOf(::DeletePasskeyUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factoryOf(::MoveToTrashUseCase)
    factoryOf(::RestoreFromTrashUseCase)
    factoryOf(::DeleteVaultItemUseCase)
    factoryOf(::EmptyTrashUseCase)
    factoryOf(::PurgeExpiredTrashUseCase)
    factoryOf(::GenerateTotpUseCase)
    factoryOf(::ParseScannedCodeUseCase)
    factoryOf(::ExportVaultUseCase)
    factoryOf(::ReadBackupInfoUseCase)
    factoryOf(::ImportBackupUseCase)
    factoryOf(::RestoreFromBackupUseCase)
    factory<BackupRestorer> { VaultBackupRestorer(get(), get()) }
    factoryOf(::GetVaultAccountIdUseCase)
    viewModelOf(::BackupViewModel)
    viewModelOf(::VaultListViewModel)
    viewModelOf(::ItemDetailViewModel)
    viewModelOf(::ItemEditViewModel)
    viewModelOf(::TrashViewModel)
    viewModelOf(::ScanCodeViewModel)
}

private val generatorModule = module {
    includes(generatorDataModule)
    factoryOf(::GeneratePasswordUseCase)
    factoryOf(::GeneratePassphraseUseCase)
    factoryOf(::GenerateUsernameUseCase)
    factoryOf(::EstimateStrengthUseCase)
    factoryOf(::ObserveGeneratorHistoryUseCase)
    factoryOf(::AddToGeneratorHistoryUseCase)
    factoryOf(::ClearGeneratorHistoryUseCase)
    viewModelOf(::GeneratorViewModel)
}

private val settingsModule = module {
    includes(settingsDataModule)
    factoryOf(::ObserveSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
    factoryOf(::GetAccountIdForSettingsUseCase)
    factoryOf(::LockNowUseCase)
    factoryOf(::DeleteAccountUseCase)
    viewModelOf(::SettingsViewModel)
}

private val autofillModule = module {
    factoryOf(::FindAutofillMatchesUseCase)
    factoryOf(::SaveAutofillCredentialUseCase)
}

val appModule = module {
    single { ComposeResourceWordlist() }
    single<WordlistSource> { get<ComposeResourceWordlist>() }
    single<GeneratorWordsSource> { get<ComposeResourceWordlist>() }
    includes(coreDataModule, onboardingModule, lockModule, vaultModule, generatorModule, settingsModule, autofillModule)
}
