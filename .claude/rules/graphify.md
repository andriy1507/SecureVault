# Graphify (knowledge graph)

## Use the graph, do not scan the tree
- At session start, read `graphify-out/GRAPH_REPORT.md` (god nodes, communities, structure) before anything else.
- Answer "where is X / how does Y connect" with graphify `query`, `path` and `explain`, not with directory listings.
- Do **not** run recursive `ls`/`find`/`Glob **/*` or read many files to orient. Open only the specific source files a graph result points to.
- `Grep`/`Glob` are for exact-symbol or exact-string lookups after the graph has narrowed the area, not for discovery.
- If `graphify-out/` does not exist yet, say so and offer `/graphify .`; do not fall back to scanning the whole tree.

## Keep the graph current
Update the graph with `/graphify . --update` after any **major** change:
- a new, removed or renamed module or top-level directory;
- a refactor that moves or splits several files;
- a new feature merged, or a dependency/architecture change;
- a large generated or imported code drop.

Small edits inside existing files do not need an update. Optional: `graphify hook install` rebuilds code changes after each commit (docs and images still need a manual `--update`).

## Notes
- `graphify-out/` is generated and git-ignored. Do not read `graphify-out/cache/`.
- Git worktrees (`.claude/worktrees/`) lack `graphify-out/`. Read it from the main checkout: the parent of `git rev-parse --path-format=absolute --git-common-dir`.
- If the graph looks stale after large changes, say so and run the update (rebuild fully only when asked).
- One-time bootstrap in a new project: `graphify claude install` (adds a graphify section to CLAUDE.md).
