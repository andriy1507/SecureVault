---
description: Build, run, test and lint commands for SecureVault. Load when building, running or fixing lint.
---

# Build and lint

- Project uses **Kotlin Toolchain (Amper)**, not Gradle. Modules are listed in `project.yaml`.
- Build all: `./kotlin build` (Windows: `kotlin.bat build`).
- Run: `./kotlin run -m androidApp`, `./kotlin run -m iosApp`.
- Lint (same as CI): `./scripts/lint.sh`. Auto-format with ktlint first: `./scripts/lint.sh --fix`. Regenerate the detekt baseline only on request: `./scripts/lint.sh --baseline`.
- Detekt config: `config/detekt/detekt.yml`; baseline: `config/detekt/baseline.xml`. Do not add new baseline entries to hide real issues.
- Tests live in `test/` and `test@android/` folders next to `src/`.
- Lint and build outputs go to `build/` and `.lint-cache/`. Do not read them; they are excluded.
