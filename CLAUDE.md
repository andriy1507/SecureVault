# SecureVault

Local-first password manager and 2FA authenticator, Kotlin Multiplatform (Android + iOS). Product context: `docs/product/CONFLUENCE.md`.

## Session start
1. Read `graphify-out/GRAPH_REPORT.md` first (if it exists). Rules: `.claude/rules/graphify.md`.

## Commands
- Build all: `./kotlin build` (Windows: `kotlin.bat build`). Kotlin Toolchain (Amper), not Gradle.
- Run: `./kotlin run -m androidApp` | `./kotlin run -m iosApp`
- Test: `./kotlin test`
- Lint (CI parity): `./scripts/lint.sh`; auto-fix: `./scripts/lint.sh --fix`

## Structure
- `androidApp/`, `iosApp/`: platform apps. `shared/`: Compose UI entry, navigation and DI (`shared/src/di/AppModule.kt`).
- `core/core-{domain,data,presentation,ui}/`: shared modules. `feature/<name>/<name>-{domain,data,presentation,ui}/`: features (autofill, generator, legal, lock, onboarding, settings, vault).
- Platform code in sibling `src@android/` and `src@ios/`. Module list: `project.yaml`

## Where things are
- Domain rules (load when relevant): `.claude/rules/`
- Plans: `docs/plans/` (empty until a plan is written). Architecture, API contracts, sequence diagrams: `docs/architecture/` (`contracts/`, `sequences/`)
- Design tokens: `docs/design/tokens.md` (tracked). `docs/design/FIGMA.html` is git-ignored and local-only, reference only

## Working rules
- Never commit secrets, keystores, recovery-key exports (`vault-recovery-key-*.txt`), `.env*` or `local.properties`.
- No network calls or analytics unless a plan in `docs/plans/` says so. The app is local-first and anonymous.
- Explore with the graphify graph, not by scanning the file tree (`.claude/rules/graphify.md`).
- After a major change, update the graph (`/graphify . --update`).
- Keep this file short. Put detail in `.claude/rules/` or `docs/`.
