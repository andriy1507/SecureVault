# SecureVault

Local-first password manager and 2FA authenticator, Kotlin Multiplatform (Android + iOS). Product context: `docs/product/CONFLUENCE.md`.

## Session start
1. Read `graphify-out/GRAPH_REPORT.md` first. Details and worktree notes: `.claude/rules/graphify.md`.

## Commands
- Build all: `./kotlin build` (Windows: `kotlin.bat build`). Kotlin Toolchain, not Gradle.
- Run: `./kotlin run -m androidApp` | `./kotlin run -m iosApp`
- Lint (CI parity): `./scripts/lint.sh`; auto-fix: `./scripts/lint.sh --fix`

## Structure
- `androidApp/`, `iosApp/`, `shared/` (Compose UI and common logic)
- `core/` and `feature/<name>/` split into `-domain`, `-data`, `-presentation`, `-ui`
- Platform code in sibling `src@android/` and `src@ios/`. Module list: `project.yaml`

## Where things are
- Domain rules (load when relevant): `.claude/rules/`
- Plans: `docs/plans/` (empty until a plan is written). Architecture, API contracts, sequence diagrams: `docs/architecture/` (empty placeholders)
- Design: `docs/design/tokens.md` (tracked). `docs/design/FIGMA.html` is git-ignored and local-only, reference only

## Working rules
- Never commit secrets, keystores, recovery-key exports or `local.properties`.
- No network calls or analytics unless a plan in `docs/plans/` says so. The app is local-first and anonymous.
- Keep this file short. Put detail in `.claude/rules/` or `docs/`.
