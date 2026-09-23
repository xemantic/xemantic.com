# Development

Notes for working on this project itself:
the conventions its documentation follows,
and the tooling and dependency updates worth doing from time to time.

## Render and serve the website

Render the whole site into `build/website`:

```shell
./gradlew run
```

Serve it live from the source, rendering Markdown on each request:

```shell
./gradlew serve
```

## Update BeerCSS and Material Symbols

BeerCSS and the Material Symbols icon font are published from the site's own origin,
so the site makes no third-party request for them.
The BeerCSS version is `beerCss` in [libs.versions.toml](gradle/libs.versions.toml),
its modules and the icons in use are listed at the end of [build.gradle.kts](build.gradle.kts).
The `generateBeerCss` task builds `assets/css/beercss.css` and `assets/fonts/material-symbols-outlined.woff2`
into `build/generated/website`, from where `run` publishes them and `serve` serves them.
It reruns, downloading the files again, only when the version, the modules or the icons change.

To use a new icon, add its name, as listed on [Material Symbols](https://fonts.google.com/icons),
to `materialSymbols` first.
Every `./gradlew run` is followed by `checkMaterialSymbols`,
which fails the build when a rendered page uses an icon missing from the font.

## Update gradlew wrapper

```shell
./gradlew wrapper --gradle-version latest --distribution-type bin
```

## Update all the dependencies to the latest versions

All the gradle dependencies are managed by the
[libs.versions.toml](gradle/libs.versions.toml) file in the `gradle` dir.

To resolve the latest versions,
and apply them automatically to [libs.versions.toml](gradle/libs.versions.toml),
run the [version-catalog-update](https://github.com/littlerobots/version-catalog-update-plugin) plugin:

```shell
./gradlew versionCatalogUpdate
```

To review and pick the updates one by one instead of applying them all,
use the interactive mode:

```shell
./gradlew versionCatalogUpdate --interactive
```

then apply the staged changes with:

```shell
./gradlew versionCatalogApplyUpdates
```

> [!NOTE]
> The plugin is configured in [build.gradle.kts](build.gradle.kts)
> to preserve the manual ordering of `libs.versions.toml` (`sortByKey = false`),
> and to keep the `kotlinTarget` and `javaTarget` version constants,
> which have no `version.ref` and would otherwise be removed as unused.

## Documentation conventions

All the Markdown files in this project are authored with
[semantic line breaks](https://sembr.org/).
Each sentence starts on its own line,
and long sentences may be split further at clause boundaries.
This keeps `git diff` and code review focused on the sentence that actually changed,
instead of on a whole reflowed paragraph.

There is no maximum line length,
and paragraphs are never hard-wrapped to a fixed column.
Line length is a rendering concern,
so it is left to the editor.

### Markdown soft wrapping in the IDE

**IntelliJ IDEA**:
`Settings` → `Editor` → `General` → `Soft Wraps`,
enable `Soft-wrap these files` and make sure the mask contains `*.md`
(the default mask already does).
To toggle it for the file at hand only,
use `View` → `Active Editor` → `Soft-Wrap`.

**VS Code**:
add the following to your `settings.json`:

```json
{
  "[markdown]": {
    "editor.wordWrap": "on"
  }
}
```

Alternatively toggle it for the current file with `Alt`+`Z` (`Option`+`Z` on macOS).
