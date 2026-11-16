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

### Kotlin

- Context-sensitive resolution (`-Xcontext-sensitive-resolution`) is enabled:
  drop the type qualifier wherever the expected type is known —
  `is Serve ->` instead of `is Resolution.Serve ->` in a `when`, `return NotFound` instead of `return Resolution.NotFound` —
  but keep it on constructor calls (`Resolution.Serve(file)`), which the feature does not cover.

## Known gotchas

- Every non-hidden file at the repository root is part of the website,
  so a new root-level file is published to xemantic.com unless it is listed in
  `excludedRootEntries` or `excludedFileNames` in `Render.kt` —
  repository documentation and build machinery belong in those sets.
- `./gradlew versionCatalogUpdate` reports configuration cache problems
  (`invocation of 'Task.project' at execution time`) and discards the cache entry,
  but the catalog is still updated correctly —
  the plugin is simply not configuration-cache compatible, so the warning is expected.
- `wrapInHtmlDocument` only moves the YAML front matter into `<head>` when the `frontmatter` mark
  is the *first* event of the stream —
  any operator emitting a mark before it (a `main` wrapper, for instance)
  turns the front matter into ordinary body content,
  rendered as a literal `<frontmatter>` element.
  `ensureFrontmatterTitle` is just as position-sensitive:
  it derives the `<title>` only from an `h1` directly following the front matter,
  so it has to run straight after `parse()`, before anything wraps the heading.
- `./gradlew run` renders into `build/website`,
  while the legacy Jekyll output still sits in the untracked `_site` directory —
  do not confuse the two when verifying a change.
- BeerCSS is not loaded from a CDN, nor through its JavaScript loader:
  `generateBeerCss` concatenates the chosen modules into `build/generated/website/assets/css/beercss.css`,
  together with a Material Symbols font subset holding only the listed icons —
  never commit a copy into `assets`, the build fails when a source and a generated file share a path.
  The inline script at the start of `body` replaces `beer.loader.js`,
  whose only effect here was setting the `dark`/`light` `body` class.
  Any new BeerCSS class used in content requires adding its module —
  the current list was verified by diffing computed styles of every rendered page against the full bundle.
- `REUSE.toml` licenses everything as CC BY 4.0 unless a later annotation says otherwise (the last matching one wins) —
  third-party material added to the site (images, quoted works, vendored libraries) needs its own annotation,
  verified with `uvx --from 'reuse[charset-normalizer]' reuse lint` (plain `uvx reuse lint` fails without an encoding-detection module).
- BeerCSS adds a `1rem` top margin to a block element (`section`, `nav`, `p`, headings, …) following any sibling,
  with a selector too specific (0,4,1) to override casually —
  an absolutely positioned helper element therefore goes *after* such a block in the markup, not before it.
- The Arsenal `@font-face` rules each repeat the same `size-adjust` value,
  which cannot be shared through a custom property —
  `var()` is invalid in `@font-face` descriptors, and the browser silently drops such a declaration.
- Google Fonts serves the `icon_names` subset as woff2 only to a browser User-Agent,
  otherwise it answers with a TrueType font.

## Anti-patterns to avoid

- Do not add content to this file that is already discoverable by reading the source or build scripts — that inflates context without adding signal, reducing AI agent task success rates (see [arxiv 2602.11988](https://arxiv.org/abs/2602.11988)).
