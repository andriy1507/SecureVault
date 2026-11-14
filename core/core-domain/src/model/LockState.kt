package dev.spaceapps.secvault.core.domain.model

/** Where the vault currently is in its lifecycle. */
enum class LockState {
    /** Storage not read yet (app just started). */
    Unknown,

    /** No vault on this device: onboarding is needed. */
    NoVault,

    /** Vault exists, keys are sealed by hardware. */
    Locked,

    /** Keys are in memory, items can be read and written. */
    Unlocked,
}
