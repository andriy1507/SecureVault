---
description: Knowledge graph use. Applies at the start of every session and for any codebase question.
---

# Graphify (knowledge graph)

**At the start of every session**, before exploring:
1. Read `graphify-out/GRAPH_REPORT.md` (god nodes, communities, structure).
2. Use the graph to answer "where is X / how does Y connect". Use the graphify query, path and explain tools when available.

**Do not read all files to orient.** Open only the specific source files a graph result points to.

Notes:
- `graphify-out/` is generated and git-ignored. Its cache (`graphify-out/cache/`) is excluded from search.
- Git worktrees (`.claude/worktrees/`) do not contain `graphify-out/`. Read it from the main checkout: the `graphify-out/` folder next to the main repo's `.git` (the parent of `git rev-parse --path-format=absolute --git-common-dir`).
- If the graph looks stale after large changes, say so. Do not rebuild it unless asked.
