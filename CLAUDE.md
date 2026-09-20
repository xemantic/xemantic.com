# CLAUDE.md

This file captures only what cannot be inferred from the codebase itself.

## Rules for editing this file

Both developers and AI agents are expected to add entries as they encounter surprises.

- **Add an entry** when you encounter something unexpected: a build quirk, a non-obvious constraint, a dependency gotcha, or any behavior that would surprise the next agent or developer.
- **Add an entry** when a developer flags an anti-pattern produced by AI — describe the anti-pattern and the preferred alternative.
- **Do not** add codebase overviews, directory listings, or anything discoverable by reading the source.
- Keep entries concise: one line per lesson, grouped under a heading if a theme emerges.

## Conventions

### Markdown authoring

Markdown files use [semantic line breaks](https://sembr.org/):
break a line after a sentence,
and optionally at clause boundaries within a long sentence,
so that diffs stay meaningful and reviewable.

There is no column width limit —
never reflow or hard-wrap a paragraph to fit some character count.
Modern editors soft-wrap Markdown visually,
see [DEVELOPMENT.md](DEVELOPMENT.md#markdown-soft-wrapping-in-the-ide) for how to enable it.

This convention applies to the Markdown sources of the website itself as well,
not only to the repository's own documentation.

## Known gotchas

- Every non-hidden file at the repository root is part of the website,
  so a new root-level file is published to xemantic.com unless it is listed in
  `excludedRootEntries` or `excludedFileNames` in `Render.kt` —
  repository documentation and build machinery belong in those sets.
- `./gradlew versionCatalogUpdate` reports configuration cache problems
  (`invocation of 'Task.project' at execution time`) and discards the cache entry,
  but the catalog is still updated correctly —
  the plugin is simply not configuration-cache compatible, so the warning is expected.
- `./gradlew run` renders into `build/website`,
  while the legacy Jekyll output still sits in the untracked `_site` directory —
  do not confuse the two when verifying a change.

## Anti-patterns to avoid

- Do not add content to this file that is already discoverable by reading the source or build scripts — that inflates context without adding signal, reducing AI agent task success rates (see [arxiv 2602.11988](https://arxiv.org/abs/2602.11988)).
