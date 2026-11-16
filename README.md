# Claude project template

Clean Claude Code setup derived from the SecureVault conventions (local-first, security-first, layered KMP).

## Use
1. Copy this folder's contents into the new project root.
2. `CLAUDE.md` and `.claude/rules/*.md` are filled in for SecureVault (Kotlin Toolchain, Koin, Compose, en + uk). For another project, replace the SecureVault names and delete rules that do not apply (e.g. `ui.md`).
3. Run `git init`, then `/graphify .` to build the graph, then `graphify claude install` and optionally `graphify hook install`.
4. Adjust `ECC_HOOK_PROFILE` in `.claude/settings.json` (`minimal` | `standard` | `strict`).

## Layout
- `CLAUDE.md`: short entry point (commands, structure, working rules).
- `.claude/rules/`: `graphify`, `security`, `architecture`, `build-and-lint`, `ui`, `ecc-hooks`.
- `.claude/settings.json`: read deny-list, ECC profile, graphify nudge hook.
- `.claude/hooks/graphify-nudge.js`: reminds Claude to use the graph instead of scanning files.
- `docs/{product,plans,architecture,design}/`: placeholders; plans gate any network/sync work.
