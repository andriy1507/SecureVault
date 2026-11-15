# Design tokens (Material 3, seed #4355B9)

Source: `FIGMA.html` (`:root` = light, `.dark` = dark). Code source of truth: `core/core-ui/src/theme/Color.kt` (`Palette`), `Dimens.kt`, `SecureVaultTheme.kt`. The values below were checked against `Color.kt` on 2026-11-16 and match.

Do not hard-code hex in screens. Use `MaterialTheme.colorScheme` roles and `Dimens`.

## Color roles

| Role | Light | Dark |
| --- | --- | --- |
| primary | #4355B9 | #BAC3FF |
| on-primary | #FFFFFF | #08218A |
| primary-container | #DEE0FF | #293CA0 |
| on-primary-container | #00105C | #DEE0FF |
| secondary-container | #E0E1F9 | #424659 |
| on-secondary-container | #181A2C | #E0E1F9 |
| tertiary-container | #FFD7F1 | #5C3F57 |
| on-tertiary-container | #2D1228 | #FFD7F1 |
| surface | #FBF8FF | #131318 |
| surface-container-low | #F5F2FA | #1B1B21 |
| surface-container | #EFEDF4 | #1F1F25 |
| surface-container-high | #EAE7EF | #2A292F |
| surface-container-highest | #E4E1E9 | #35343A |
| on-surface | #1B1B21 | #E4E1E9 |
| on-surface-variant | #46464F | #C6C5D0 |
| outline | #767680 | #90909A |
| outline-variant | #C6C5D0 | #46464F |
| error | #BA1A1A | #FFB4AB |
| success (custom) | #1E6C3A | #7DDC9A |
| inverse-surface | #303036 | #E4E1E9 |
| inverse-on-surface | #F2EFF7 | #303036 |
| inverse-primary | #BAC3FF | #4355B9 |

Custom roles not in the M3 scheme: `success`, `kbd`, `kbd-key`. Add them to the theme only if a screen needs them.

## Typography

- UI: Roboto 400 / 500 / 700
- Passwords and IDs: Roboto Mono 500 (password 24 px, account ID 20 px, letter-spacing 0.5 px)
- Icons: Material Symbols Rounded, 24 px

## Shape and spacing

- Status tag radius: 8 px
- Spacing and sizes: use `Dimens.dp*` (4, 8, 12, 16, 20, 24, 32, 44, 48 …)
