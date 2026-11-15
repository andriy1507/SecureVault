---
description: Security and cryptography rules for vault data, keys, clipboard and logging. Load for any crypto, storage, autofill or passkey change.
---

# Security and crypto

- Never log secrets: passwords, TOTP seeds, recovery phrases, keys, backup payloads. `PasskeyLog.kt` is the only logging pattern for passkey flows; reuse it, do not add `println`/`Log` calls with item data.
- Items are encrypted per item before storage (`core/core-data/src/cipher/VaultCipher.kt`). Backups use `BackupCipher.kt`. Do not add a second cipher path.
- Key hierarchy and bundles: `core/core-data/src/crypto/KeyHierarchy.kt`, `KeyBundle.kt`. Keys are sealed by hardware (`HardwareKeyGuard`, Android and iOS actuals). Never store raw key material in preferences or the database.
- Randomness: use `CryptoSecureRandom` / `SecureRandomSource`, never `kotlin.random` or `java.util.Random`.
- Clipboard: copy secrets only through `SecureClipboard` so they auto-clear.
- Screenshots are blocked by default on Android. Do not remove `FLAG_SECURE` without a plan.
- Recovery-key exports are `vault-recovery-key-*.txt`. They are git-ignored and must never be committed or added to fixtures.
- No analytics, crash reporters, or third-party network calls. The app is anonymous by design.
- Tests for crypto go in `core/core-data/test@android/`. Use fixed test vectors, never real keys.
