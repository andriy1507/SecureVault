# SecureVault

Local-first password manager and 2FA authenticator, Kotlin Multiplatform (Android + iOS). Product context: `docs/product/CONFLUENCE.md`.

## Session start
1. Read `graphify-out/GRAPH_REPORT.md` and query the graph. Do not read every source file to orient. Details: `.claude/rules/graphify.md`.

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
- Plans: `docs/plans/`. Architecture, API contracts, sequence diagrams: `docs/architecture/`
- Design: `docs/design/FIGMA.html` (reference only), tokens in `docs/design/tokens.md`

## Working rules
- Never commit secrets, keystores, recovery-key exports or `local.properties`.
- No network calls or analytics unless a plan in `docs/plans/` says so. The app is local-first and anonymous.
- Strings go in both `values/` and `values-uk/` of the module's `composeResources`.
- Keep this file short. Put detail in `.claude/rules/` or `docs/`.
