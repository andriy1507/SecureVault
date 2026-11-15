# SecureVault — Product Overview

**Owner:** Andriy Goryachok · **Status:** MVP implemented (Android verified, iOS untested); encrypted backup and restore implemented on branch `feature/import-export` (not merged to main yet); Figma rebuild in progress · **Updated:** Nov 15, 2026

## Summary

SecureVault (shown to users as "Vault") is a local-first password manager and 2FA authenticator for Android and iOS, built with Kotlin Multiplatform. People sign up with an anonymous account ID instead of an email or phone number, and keep a 24-word recovery key that only they hold.

The local-only MVP is implemented: onboarding, phone-unlock lock screen, vault (logins, cards, notes, trash), 2FA codes with QR scanning, passkeys (Android), password generator and settings. An encrypted backup file (export, restore on a new phone, import into an existing vault) is implemented on the `feature/import-export` branch and still needs no server. Cloud sync, device linking and sharing come later, and sync stays optional.

## Goals and non-goals

The product wins when a person can keep every password and 2FA code in one place without giving anyone their identity or trusting a server with readable data.

**Goals**

1. Store logins, payment cards and secure notes, end-to-end encrypted, usable fully offline.
2. Replace a separate authenticator app: 2FA (TOTP) codes live next to the logins they protect.
3. Sign up with no email, no phone number and no master password: an anonymous ID plus a recovery key.
4. Autofill passwords system-wide and generate strong ones, like Bitwarden (Android first).
5. One shared codebase for Android and iOS, light and dark themes, following the team's KMM guidelines.
6. Encrypted backup file, no server needed: export and restore on a new device (done on `feature/import-export`). Later: optional cloud sync, linking devices with Element-style verification, and sharing items with trusted contacts.

**Non-goals (for now)**

- Tablets and desktop apps.
- A master password or account recovery by the service: if the recovery key is lost, the vault is lost.
- Mandatory cloud: the app must stay fully usable without ever turning sync on.
- Browser extensions and team or enterprise administration.

## Product principles

Every feature decision is checked against these five rules.

- **Local-first.** The device is the source of truth. Everything works offline; the cloud only syncs and backs up.
- **Cloud is optional.** Sync is opt-in and off by default. Linking a device or restoring must also work without a server (QR between devices, encrypted backup file).
- **Anonymous by design.** A random account ID like 7F3A-91C2-E04B-D58E replaces email and phone. We never learn who the user is.
- **No master password.** Vault keys are generated, sealed by device hardware and opened with the phone's own unlock. The 24-word recovery key is the only backup, and only the user has it.
- **Nothing readable leaves the device.** Items are encrypted per item before they are stored; clipboard copies of secrets clear themselves; screenshots are blocked by default on Android.

## Personas

Three people shape the MVP; the user stories below are written for them.

| Persona | Who they are | What they need most |
| --- | --- | --- |
| Privacy-minded individual | Avoids giving email or phone to services; uses one phone | Anonymous account, local-only storage, no tracking |
| Busy everyday user | Reuses passwords today, juggles a separate authenticator app | Autofill, generator, 2FA codes in the same app, fast unlock |
| Multi-device user (later) | Phone plus a second phone or tablet | Optional sync, safe device linking, restore after losing a phone |

## MVP scope

The MVP is a local-only app: everything below works on one device with no server. It follows the design file, minus the screens that need the cloud.

**In the MVP (implemented)**

- Onboarding: Welcome, Terms and Privacy, account ID, 24-word recovery key (copy, save as .txt), confirm-by-chips.
- Lock: "Vault is locked" screen using the phone's own unlock (no app PIN yet).
- Vault: logins, cards, notes; search, filters, favorites, sort; item actions sheet; trash with undo; password history.
- 2FA codes: QR scanner, manual key, live codes list, codes inside logins.
- Generator: password, passphrase, username, session history.
- Passkeys (Android 14+): Vault is a Credential Manager passkey provider. Creating saves the passkey on the matching login (or a new one); signing in lists passkeys after unlock; the login detail shows passkeys and can delete them. Settings has a shortcut to the system's passkey provider screen.
- Settings: account ID, autofill (Android), theme, dynamic color, auto-lock, clipboard clearing, block screenshots, lock now, about, delete account.

**Implemented on `feature/import-export` (not yet merged to main)**

- Backup export: all items (not trash) saved to an encrypted `.svbackup` file that only the account's 24-word recovery key can open.
- Restore on a new device during onboarding, keeping the same account ID and keys.
- Import a backup into an existing vault from Settings, skipping duplicates (same title, username and website).
- Shared recovery phrase input that also accepts the saved recovery key `.txt` file.

**Left out of the MVP**

- Needs the cloud: link a new device, verify device, trusted devices, cloud sync, sync conflicts, sharing and contacts.
- Other: import from other password managers or CSV, CSV export, in-app language picker, autofill setup screen, own app PIN, folders, identity and SSH key item types.

## User stories

18 of 24 stories are in the MVP and implemented. US-07 is implemented on `feature/import-export` (not merged yet), and US-23 is partly done (own encrypted backup file); the other stories are marked Later and need cloud sync or more work. Each story is written as "As a user, I want…, so that…".

| ID | Epic | Story | Acceptance criteria | Release |
| --- | --- | --- | --- | --- |
| US-01 | Onboarding | I create an account without email or phone, so nobody can tie the vault to me. | Welcome → Create new account; ID shown as XXXX-XXXX-XXXX-XXXX with Copy. | MVP · done |
| US-02 | Onboarding | I save a recovery key, so I can get my vault back if I lose the phone. | 24 words in a grid; Copy (auto-clears) and Save as .txt; Continue enabled only after "I've saved my recovery key". | MVP · done |
| US-03 | Onboarding | I prove I wrote the key down, so I don't lose access by mistake. | 3 random word slots filled by tapping chips among decoys; wrong words reset with an error. | MVP · done |
| US-04 | Onboarding | I can read the Terms and Privacy Policy before I start. | Links on Welcome and in Settings → About. | MVP · done |
| US-05 | Unlock | I open the vault with my phone's screen lock or biometrics, so I don't need another password. | "Vault is locked" screen with masked ID; system prompt opens automatically; Unlock button retries. | MVP · done |
| US-06 | Unlock | The vault locks itself when I leave it, so a borrowed phone doesn't expose it. | Auto-lock: immediately, 1, 5, 15 min, or when the phone locks. | MVP · done |
| US-07 | Unlock | I restore my vault on a new phone with my ID and recovery key. | Restore an encrypted backup file during onboarding with the 24 words; same account ID and keys. Sync restore comes later. | MVP branch · done (`feature/import-export`, not merged) |
| US-08 | Vault | I save logins, payment cards and secure notes. | Type picker; login: name, username, password, websites, 2FA secret, notes; card with live preview; note. | MVP · done |
| US-09 | Vault | I find an item fast. | Search, filter chips All / Logins / Cards / Notes / 2FA codes, Favorites section, sort by name, date added, last modified. | MVP · done |
| US-10 | Vault | I copy a password without leaving it on the clipboard. | Copy shows "Password copied · clears in 30 s"; clear time set in Settings. | MVP · done |
| US-11 | Vault | I can undo a delete. | Delete moves to Trash with an Undo snackbar; Trash restores or deletes forever; items purge after 30 days. | MVP · done |
| US-12 | Vault | I can find a password I changed recently. | Last 10 passwords kept per login under Password history. | MVP · done |
| US-13 | 2FA codes | I scan a website's 2FA QR code instead of using Google Authenticator. | Camera scanner; otpauth link fills name, account and secret; non-2FA QR codes are rejected. | MVP · done |
| US-14 | 2FA codes | I see my current codes at a glance. | 2FA codes list refreshes every second with a countdown ring; tap a row to copy. | MVP · done |
| US-15 | 2FA codes | I add a code without a camera. | Enter key manually; a base32 key or otpauth link both work. | MVP · done |
| US-16 | Generator | I generate strong passwords, passphrases and usernames. | Length 8–64 and character sets; 3–10 words with separator; strength meter; session history cleared on lock. | MVP · done |
| US-17 | Autofill | Logins fill in other apps and websites, and new ones are offered for saving (Android). | Unlock entry in the autofill list; strong-password suggestion on sign-up forms; save prompt after sign-in. | MVP · done |
| US-18 | Settings | I choose how the app looks and behaves. | Theme light / dark / system, dynamic color (Android 12+), block screenshots, lock now. | MVP · done |
| US-19 | Settings | I erase my account from this device. | Delete account dialog, enabled only after typing DELETE. | MVP · done |
| US-20 | Sync | I can turn on encrypted sync and backup, but never have to. | Off by default; app fully usable without it. | Later |
| US-21 | Devices | I link a new device safely. | QR pairing code plus Element-style emoji comparison; trusted devices list. | Later |
| US-22 | Sharing | I share an item with someone I trust. | Contacts by account ID with emoji verification; view or edit permission. | Later |
| US-23 | Data | I move my passwords in or out. | Own encrypted backup export and import (done on `feature/import-export`); import Bitwarden, 1Password, Google, Apple, CSV and export CSV still to do. | Partly · Later for the rest |
| US-24 | Settings | I pick the app language. | Language list; today the app follows the system language. | Later |

## Roadmap after the MVP

Next comes polish; cloud features follow in four phases, and sync stays opt-in throughout.

| Phase | Status | Contents |
| --- | --- | --- |
| MVP | Done | Local vault, 2FA codes, generator, settings |
| Polish | Next | Merge backup branch, iOS on device, new item types, third-party import and CSV export, language picker |
| Backup and sync | Opt-in only | Encrypted backup file (built on `feature/import-export`, not merged yet); cloud sync, sync conflicts, restore from server |
| Devices | Later | Link via QR, emoji verification, trusted devices, remove device |
| Sharing | Later | Contacts by ID, share items, view or edit rights, invites |

Phases have no dates yet; device linking and sharing depend on the sync backend chosen in Backup and sync.

Also planned for later: a UI polish pass comparing every screen against the design (spacing, typography, animations and transitions, empty and error states, dark theme, iOS look, accessibility and large fonts).

## Security model

The vault key never leaves the device unencrypted, and nothing can open it without the phone's unlock or the user's recovery key.

- **Key hierarchy.** Each item is encrypted with its own AES-256-GCM key; item keys are wrapped by one vault key. Ciphertext is bound to its item ID, so stored rows can't be swapped.
- **Hardware sealing.** The vault key, identity keys (P-256) and device keys are sealed by a non-exportable hardware key: Android Keystore (StrongBox when present) or the iOS Secure Enclave. Each unlock needs the phone's PIN, pattern, password or strong biometrics.
- **Recovery key.** 24 BIP39 words (256-bit) derive a wrap key that encrypts the identity and vault keys. It is shown once, confirmed by the user, and never stored.
- **Backup file.** Export writes an `.svbackup` file. Items are encrypted with a backup key derived from the vault key, and the account ID is bound to the ciphertext. The file also carries identity and vault keys encrypted with the recovery phrase, so only the 24 words open it, on any device. Trash is not exported.
- **Account ID.** First 8 bytes of SHA-256 of the identity signing key, as 16 hex characters. Anonymous and stable across devices.
- **On-device hygiene.** Decrypted data lives only in screen-scoped memory and is dropped on lock; copied secrets clear after a set time and are marked sensitive; FLAG\_SECURE blocks screenshots on Android; 2FA QR codes are decoded on the device.
- **Passkeys.** ES256 (P-256) keys created in software and stored encrypted with their login, so they're covered by the recovery key and future sync (backup-eligible flags set). Every use needs the phone's unlock (user verification). Attestation is "none" and the counter stays 0. Only browsers on Google's privileged list may claim a website origin, and that origin must match the passkey's site; other apps sign with their signing-certificate hash as origin, which the website's server checks. While the vault is locked, the system only sees "Unlock Vault", never which sites have passkeys.
- **If the screen lock or biometrics change,** Android destroys the hardware key and only the recovery key restores access.

## Architecture and tech stack

SecureVault is one Kotlin Multiplatform project with 27 modules, layered by clean architecture: View → ViewModel → UseCase → Repository → local storage.

| Layer | Modules | Role |
| --- | --- | --- |
| Apps | androidApp, shared, iosApp | MainActivity and autofill service; app root, navigation, Koin DI; Swift host and Secure Enclave guard |
| UI | onboarding-, lock-, vault-, generator-, settings-, legal-ui, core-ui | Compose Multiplatform screens; theme, Dimens, components |
| Presentation | \*-presentation, core-presentation | One ViewModel per screen with an MVI Contract (Intent in, State and Effect out) |
| Domain | core-domain and \*-domain | Use cases and repository interfaces, pure Kotlin |
| Data | vault-data, settings-data, generator-data, core-data | Encrypted Room rows, TOTP, DataStore, crypto, hardware key guard |
| Hardware | Android Keystore, iOS Secure Enclave | Seal the vault key; phone unlock on every open |

Each feature is split into -domain, -data, -presentation and -ui modules; the shared module wires them together with Koin and simple screen-scoped navigation.

| Area | Choice |
| --- | --- |
| Language and targets | Kotlin Multiplatform: Android (min SDK 30), iOS arm64 and simulator |
| Build | Kotlin Toolchain (Amper), module.yaml per module, ./kotlin wrapper |
| UI | Compose Multiplatform, Material 3, Material icons; compose resources for strings |
| DI | Koin |
| Storage | Room 3 (encrypted item rows), DataStore (keys metadata, settings) |
| Crypto | cryptography-kotlin (platform JCA / CryptoKit) |
| Camera / QR | CameraX + ZXing (Android), AVFoundation (iOS) |
| Networking (later) | Ktor |

## Design and links

The UI follows one Material 3 design file covering 62 screens on Android and iOS, light and dark.

| What | Where | Notes |
| --- | --- | --- |
| UI design (62 screens) | [secure-vault-ui](https://claude.ai/artifact/MCA4Ngo3CJB5hsbiW8zPo6) | Material 3, seed color #4355B9, Material Symbols icons |
| Figma design system and screens | [SecureVault (Figma)](https://www.figma.com/design/cV0xZjlobsKjcdJ9k8HrSV/SecureVault) | Pages: Imported (reference), Design system (variables, text styles, 19 components), Screens (rebuilt from components; in progress). Interactive prototype planned. |
| Code | Android Studio project SecureVault, hosted on [GitHub](https://github.com/andriy1507/SecureVault) | Kotlin Toolchain (Amper), ./kotlin build |
| Implementation status sheet | [SecureVault – first slice status](https://claude.ai/artifact/THz3cMLnvCHjQm2iw5gvfB) | From the first implementation slice |
| Confluence page | This page | Source of truth for the product overview |
| Repository | [andriy1507/SecureVault](https://github.com/andriy1507/SecureVault) (GitHub) | Public; source of truth for the code once pushed |

UI rules in the code: every size comes from Dimens, every color from the theme palette, every text from strings.xml (compose resources per feature module).

Figma tokens mirror the code: the Color collection matches the theme palette (a Color Dark collection holds dark values, because the free Figma plan allows one mode per collection), the Dimens collection matches Dimens (spacing/dpN, radius/\*), and the text styles use the Material 3 type scale in Roboto. Icons are Material Symbols Rounded, set by typing the icon name.

## Decisions and open questions

Decisions made so far, newest first.

| Date | Decision | Why |
| --- | --- | --- |
| 2026-11-15 | Backup is an encrypted `.svbackup` file that only the 24-word recovery key opens; restore and import work without a server | Gives a way to move to a new phone and keep data local-first, with no server and no new secret to remember |
| 2026-11-15 | Import into an existing vault skips duplicates (same title, username and website) | Restoring the same backup twice must not create copies |
| 2026-11-14 | Vault is a passkey provider: passkeys live on login items as synced (vault-stored) keys; Android via Credential Manager first, iOS credential provider extension later | Passwords, 2FA and passkeys stay together and move with the vault; works with any site on webauthn.io-style flows |
| 2026-11-14 | Figma file is the design source: screens are built from shared components bound to Color and Dimens variables that mirror the code | One change updates every screen; design tokens match Compose theme and Dimens |
| 2026-11-14 | QR scanning uses CameraX + ZXing on Android and AVFoundation on iOS, not Google ML Kit or the Play services code scanner | The 2FA secret is decoded inside the app, with no Google telemetry; works on phones without Play services. ML Kit (bundled) stays an option if scanning proves unreliable |
| 2026-11-14 | Vault doubles as a 2FA authenticator: QR scan, manual key, live codes list | Replace Google Authenticator; codes live next to their logins |
| 2026-11-14 | Cloud sync is optional, never mandatory | Local-first; the app must be fully usable offline |
| 2026-11-14 | Keep phone unlock for vault access; an own app PIN is deferred | Don't rework the security layer for the MVP |
| 2026-11-14 | Recovery key stays 24 words; exported as .txt, not PDF | 256-bit key; plain file the user controls |
| 2026-11-14 | Account ID is 16 characters, XXXX-XXXX-XXXX-XXXX | Matches the design; harder to collide |
| 2026-11 | Shared Compose Multiplatform UI instead of SwiftUI; Kotlin Toolchain (Amper) build | One UI codebase while learning KMP |
| 2026-11 | Local-only MVP first, cloud later | Ship the core without a server |

**Open questions**

- \[ \] App name: "Vault" (design) or "SecureVault" (project, launcher label)?
- \[ \] Own app PIN on top of phone unlock: when, and does it ever take part in encryption?
- \[ \] 2FA codes: keep the filter chip, or give them a fourth bottom tab?
- \[ \] Sync backend and hosting: self-hosted server, or file-based backup first?
- \[ \] Restore without a server: encrypted backup file is built (`feature/import-export`); also add device-to-device QR?
- \[ \] Passkeys follow-ups: iOS credential provider extension (iOS 17+, after iOS runs on a device; decide whether to register site names in the system identity store); Digital Asset Links check for native apps; refreshing the bundled privileged-browser list.
- \[ \] Crash reporting: opt-in Firebase Crashlytics or self-hosted Sentry/GlitchTip? Contents stay safe if we never set a user ID, add no custom logs or keys with app data, and keep error messages free of item data, but Crashlytics still ties a Firebase installation ID, device and IP to "uses Vault". If Crashlytics: off by default with a Settings switch, never the account ID as user ID, code obfuscation on.
- \[ \] Push for sync and sharing (later): FCM only as content-free "wake up and sync" pings, so no titles, names or IDs pass through Google; the app fetches encrypted data from our server. FCM needs Play services, so add a fallback (periodic sync or UnifiedPush) for phones without Google. Same content-free rule for Apple push on iOS.
- \[ \] Hardware security key (YubiKey over NFC or USB) as an extra unlock factor for the vault? Suggested later, together with sync.
- \[ \] More item types (SSH keys, Wi-Fi, identities, API keys) or custom fields on every item, or both?
