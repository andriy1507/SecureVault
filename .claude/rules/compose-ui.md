---
description: Compose UI conventions for SecureVault screens, theme tokens, strings and previews. Load when editing any *Screen.kt or theme file.
---

# Compose UI

- Colors: only `MaterialTheme.colorScheme` roles. Never hard-code hex. Token values are in `docs/design/tokens.md`; the palette lives in `core/core-ui/src/theme/Color.kt`.
- Sizes: only `Dimens.dpN` or a named `Dimens` value. Never write `16.dp` in a screen. Add a new `Dimens` entry if a size is missing.
- Shared components live in `core/core-ui/src/components/` (Bars, Dialogs, Fields, Lists). Reuse them before writing new ones.
- Strings: every user-facing string goes in `composeResources/values/strings.xml` **and** `values-uk/strings.xml` for the same module.
- Screens are split: `*Screen.kt` (UI, stateless composables) and `*ViewModel.kt` / `*Contract.kt` (state). Screens take state and lambdas, never a ViewModel directly, so previews work.
- Previews: keep them in the same file or `CardPreview.kt`-style files, using `SecureVaultTheme` in both light and dark.
- Design reference: `docs/design/FIGMA.html` (open only when matching a screen to its design). Do not copy its inline CSS into Compose.
