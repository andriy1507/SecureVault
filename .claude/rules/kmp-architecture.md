---
description: Module layering and multiplatform source rules. Load when adding modules, files, or platform code.
---

# KMP architecture

Layers per feature: `feature/<name>/<name>-domain` (pure logic, models, use cases, repository interfaces) → `-data` (implementations) → `-presentation` (MVI contract + ViewModel) → `-ui` (Compose screens). Core modules (`core/core-domain`, `core-data`, `core-presentation`, `core-ui`) are shared by features.

- Domain modules have no Android or Compose imports.
- Presentation uses the MVI pattern in `core/core-presentation/src/mvi/`. Add a `*Contract.kt` (state, intents, effects) next to each ViewModel.
- Platform code goes in sibling folders: `src@android/`, `src@ios/`. Common code stays in `src/`. Declare `expect`/`actual` pairs with the same file names (e.g. `SecureClipboard.kt` ↔ `SecureClipboard.android.kt`).
- Dependency injection: Koin, wired in `shared/src/di/AppModule.kt` and per-module `*Module.kt` files.
- New module: add it to `project.yaml` and create a `module.yaml`, copying an existing sibling.
- Sync and device linking are not implemented (planned later; see `docs/product/CONFLUENCE.md`). Do not add network code without a plan in `docs/plans/`.
