# Build and lint

- Build tool: **Kotlin Toolchain (Amper)**, not Gradle. Modules are listed in `project.yaml`; each module has a `module.yaml`.
- Build all: `./kotlin build` (Windows: `kotlin.bat build`). Run: `./kotlin run -m androidApp`, `./kotlin run -m iosApp`. Test: `./kotlin test`.
- Lint (same as CI, `.github/workflows/code-quality.yml`): `./scripts/lint.sh` (ktlint + detekt). Auto-format with ktlint first: `./scripts/lint.sh --fix`.
- Detekt config: `config/detekt/detekt.yml`; baseline: `config/detekt/baseline.xml`. Regenerate the baseline only on request (`./scripts/lint.sh --baseline`). Do not add baseline entries to hide real issues.
- Lint and build outputs (`build/`, `.lint-cache/`) are excluded from reads; do not open them. Lint reports land in `build/reports/lint/`.
