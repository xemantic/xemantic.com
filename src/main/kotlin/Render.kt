/*
 * Copyright 2026 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.website

import com.xemantic.markanywhere.SemanticEvent
import com.xemantic.markanywhere.html.ensureFrontmatterTitle
import com.xemantic.markanywhere.html.wrapInHtmlDocument
import com.xemantic.markanywhere.html.wrapInSections
import com.xemantic.markanywhere.parse.parse
import com.xemantic.markanywhere.render.asHtml
import com.xemantic.markanywhere.yaml.parseYaml
import com.xemantic.markanywhere.yaml.renderYaml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.readString
import kotlinx.io.writeString

/**
 * Converts this flow of Markdown into a flow of HTML, moving the
 * YAML front matter into a `<head>` element. A page without a `title`
 * in its front matter (or without any front matter) gets one derived
 * from its first `h1`, so every rendered page carries a `<title>`.
 * The `head` is completed with the metadata of the [page], see
 * [addPageMetadata].
 * The document is prefixed with the HTML5 doctype, otherwise browsers
 * would fall back to the quirks mode, where the BeerCSS body grid
 * clamps `main` to the viewport height, clipping the page content.
 */
fun Flow<String>.renderMarkdownToHtml(page: Page): Flow<String> = parse()
    .ensureFrontmatterTitle()
    .wrapInSections(tocDepth = 6)
    .wrapInHtmlDocument()
    .addPageMetadata(page)
    .wrapBodyContentInMain()
    .applyPageLayout()
    .asHtml()
    .onStart { emit("<!DOCTYPE html>\n") }

/**
 * Root entries of the repository which are not a part of the website:
 * the build machinery, the licensing files and the Jekyll leftovers.
 */
internal val excludedRootEntries = setOf(
    "LICENSE",
    "LICENSES",
    "REUSE.toml",
    "_config.yml",
    "_includes",
    "_layouts",
    "_plugins",
    "_site",
    "Gemfile",
    "Gemfile.lock",
    "build",
    "build-logic",
    "gradle",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "settings.gradle.kts",
    "build.gradle.kts",
    "src",
    "update_ai_index.sh"
)

/**
 * Files documenting the repository itself, excluded at any level.
 * Hidden files are excluded as well.
 */
internal val excludedFileNames = setOf(
    "CLAUDE.md",
    "DEVELOPMENT.md",
    "README.md",
    "TODO.md"
)

/**
 * Renders the whole website: every Markdown file becomes an HTML file
 * at the same relative path, and is also copied verbatim, so that the
 * source Markdown remains addressable next to the rendered page.
 * An `ai/index.md` is additionally copied to `ai.md`, following the
 * convention of appending `.md` to a page URL to retrieve its source.
 * All the other files are copied as they are. On top of that the
 * target directory receives a `.nojekyll` marker and an `llms.txt`
 * index of all the Markdown sources.
 *
 * @param args the source directory (website root), the target directory,
 *   the URL the site will be published under (an origin, like
 *   `https://xemantic.com`, as the pages link to their assets by
 *   root-relative paths), and optionally the directories of generated files,
 *   like the BeerCSS stylesheet, published as if they were a part of the
 *   source directory.
 */
fun main(args: Array<String>) {
    val sourceDir = Path(args[0])
    val targetDir = Path(args[1])
    val siteUrl = args[2].removeSuffix("/")
    val generatedDirs = args.drop(3).map { Path(it) }
    val siteRoots = listOf(sourceDir) + generatedDirs
    val markdownPaths = mutableListOf<String>()
    var pageCount = 0
    var assetCount = 0
    runBlocking(Dispatchers.Default) {
        collectSiteFiles(sourceDir, generatedDirs).forEach { (relativePath, sourceFile) ->
            if (relativePath.endsWith(".md")) {
                pageCount++
                val htmlPath = relativePath.removeSuffix(".md") + ".html"
                val pageUrl = siteUrl + relativePath.markdownPagePath()
                val page = Page(relativePath, siteUrl, siteRoots)
                launch {
                    renderPage(sourceFile, Path(targetDir, htmlPath), page)
                    println("Rendered $htmlPath")
                }
                launch(Dispatchers.IO) {
                    copyMarkdown(sourceFile, Path(targetDir, relativePath), pageUrl)
                }
                val alias = relativePath.toFlatMarkdownAlias()
                if (alias != null) {
                    launch(Dispatchers.IO) {
                        copyMarkdown(sourceFile, Path(targetDir, alias), pageUrl)
                    }
                }
                markdownPaths += relativePath
            } else {
                assetCount++
                launch(Dispatchers.IO) {
                    copyFile(sourceFile, Path(targetDir, relativePath))
                }
            }
        }
    }
    writeTextFile(Path(targetDir, ".nojekyll"), "")
    writeTextFile(Path(targetDir, "llms.txt"), llmsTxt(siteUrl, markdownPaths))
    println("Rendered $pageCount pages and copied $assetCount assets to $targetDir")
}

/**
 * The flat Markdown alias of this relative path: `ai/index.md`
 * becomes `ai.md`, matching the `/ai` page URL with `.md` appended.
 * Non-index files already match this convention, and the root
 * `index.md` has no flat form, therefore `null`.
 */
internal fun String.toFlatMarkdownAlias(): String? =
    if (endsWith("/index.md")) removeSuffix("/index.md") + ".md"
    else null

/**
 * The `llms.txt` index of the site published under [siteUrl], listing the
 * Markdown sources found in [sourceDir] and the [generatedDirs]. Serving
 * and building share this function.
 */
internal fun llmsTxt(
    siteUrl: String,
    sourceDir: Path,
    generatedDirs: List<Path>
): String = llmsTxt(
    siteUrl,
    collectSiteFiles(sourceDir, generatedDirs).keys.filter { it.endsWith(".md") }
)

/**
 * The `llms.txt` index of the Markdown documents at [markdownPaths],
 * relative to the site root, each linked by its flat alias when it has one.
 */
private fun llmsTxt(
    siteUrl: String,
    markdownPaths: Collection<String>
): String = buildString {
    append("# ${siteUrl.substringAfter("://")}\n\n## Pages\n\n")
    markdownPaths.map { it.toFlatMarkdownAlias() ?: it }.sorted().forEach { path ->
        append("- [${path.markdownPagePath()}]($siteUrl/$path)\n")
    }
}

/**
 * The site-root-relative URL path of the page this Markdown document at
 * this relative path is published at: the root `index.md` is `/`, an
 * `index.md` within a directory is that directory, and every other file
 * drops its `.md` suffix. The path always starts with `/`, so appending it
 * to a base URL yields the document's canonical page URL.
 */
internal fun String.markdownPagePath(): String {
    val path = removeSuffix(".md")
    return when {
        path == "index" -> "/"
        path.endsWith("/index") -> "/" + path.removeSuffix("/index")
        else -> "/$path"
    }
}

/**
 * The site-root-relative URL path of the rendered HTML page of the Markdown
 * document at this relative path, as GitHub Pages serves it: an `index.md`
 * is served at its directory, with the trailing slash (the form without it
 * redirects), and every other file drops its `.md` suffix.
 */
internal fun String.htmlPagePath(): String {
    val path = removeSuffix(".md")
    return when {
        path == "index" -> "/"
        path.endsWith("/index") -> "/" + path.removeSuffix("index")
        else -> "/$path"
    }
}

/**
 * Decorates a Markdown document for publishing: its front matter is given a
 * `url` entry naming the [pageUrl] the document originates from, and a
 * `lang` entry of the [DEFAULT_LANG] unless it already states one — inserted
 * into an existing front matter, or added as a fresh one when the document
 * has none — and the site [MARKDOWN_COPYRIGHT_FOOTER] is appended. Serving
 * and building share this function, so the only intended difference between
 * the preview and the published file is the [pageUrl] host.
 *
 * Only the front matter is processed as YAML events, the Markdown body is
 * kept verbatim: rendering the parsed body back would reformat it.
 * A front matter without a closing `---` is malformed and left untouched.
 */
internal suspend fun decorateMarkdown(content: String, pageUrl: String): String {
    val lines = content.split("\n")
    val hasFrontMatter = lines.first().trimEnd() == "---"
    val close = if (hasFrontMatter) {
        lines.drop(1).indexOfFirst { it.trimEnd() == "---" } + 1
    } else 0
    if (hasFrontMatter && close == 0) return content + MARKDOWN_COPYRIGHT_FOOTER
    val yaml = if (hasFrontMatter) {
        flowOf(lines.subList(1, close).joinToString("\n")).parseYaml()
    } else {
        emptyFlow()
    }
    val frontMatter = yaml.withProvenance(pageUrl).renderYaml()
    val body =
        if (hasFrontMatter) lines.subList(close + 1, lines.size).joinToString("\n")
        else "\n$content"
    return "---\n$frontMatter---\n$body$MARKDOWN_COPYRIGHT_FOOTER"
}

/**
 * Sets the top-level `url` entry of these YAML events to [pageUrl] as the
 * first entry, dropping any previous `url`, and adds a `lang` entry of the
 * [DEFAULT_LANG] after it when no top-level `lang` is present.
 */
private suspend fun Flow<SemanticEvent>.withProvenance(
    pageUrl: String
): Flow<SemanticEvent> {
    val nodes = toList().topLevelNodes()
    val lang =
        if (nodes.any { it.isEntry("lang") }) emptyList()
        else yamlEntry("lang", DEFAULT_LANG)
    val rest = nodes.filterNot { it.isEntry("url") }.flatten()
    return (yamlEntry("url", pageUrl) + lang + rest).asFlow()
}

/**
 * Groups these events into top-level nodes: each is either a whole
 * marked subtree, like an `entry` with its value, or a lone text event.
 */
private fun List<SemanticEvent>.topLevelNodes(): List<List<SemanticEvent>> {
    val nodes = mutableListOf<List<SemanticEvent>>()
    var node = mutableListOf<SemanticEvent>()
    var depth = 0
    forEach { event ->
        node += event
        when (event) {
            is SemanticEvent.Mark -> depth++
            is SemanticEvent.Unmark -> depth--
            is SemanticEvent.Text -> {}
        }
        if (depth == 0) {
            nodes += node
            node = mutableListOf()
        }
    }
    if (node.isNotEmpty()) nodes += node
    return nodes
}

private fun List<SemanticEvent>.isEntry(key: String): Boolean =
    (firstOrNull() as? SemanticEvent.Mark)?.let {
        it.name == "entry" && it["key"] == key
    } ?: false

private fun yamlEntry(key: String, value: String): List<SemanticEvent> = listOf(
    SemanticEvent.Mark("entry", attributes = mapOf("key" to key)),
    SemanticEvent.Text(value),
    SemanticEvent.Unmark("entry")
)

private fun writeTextFile(file: Path, content: String) {
    file.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(file).buffered().use { sink ->
        sink.writeString(content)
    }
}

/**
 * Collects relative paths of all the website source files under this
 * directory, skipping hidden files everywhere, and the repository
 * infrastructure at the root level.
 */
/**
 * The files to publish, keyed by their path relative to the website root:
 * those of the [sourceDir] and of every one of the [generatedDirs].
 * A path provided twice fails the build, as it usually means a stale copy
 * of a generated file left in the source.
 */
private fun collectSiteFiles(
    sourceDir: Path,
    generatedDirs: List<Path>
): Map<String, Path> = buildMap {
    (listOf(sourceDir) + generatedDirs).forEach { dir ->
        dir.collectSiteFiles().forEach { relativePath ->
            val previous = put(relativePath, Path(dir, relativePath))
            check(previous == null) {
                "$relativePath is provided by both $previous and ${Path(dir, relativePath)}"
            }
        }
    }
}

private fun Path.collectSiteFiles(
    relativeDir: String = "",
    paths: MutableList<String> = mutableListOf()
): List<String> {
    val dir = if (relativeDir.isEmpty()) this else Path(this, relativeDir)
    SystemFileSystem.list(dir).forEach { child ->
        val name = child.name
        if (
            name.startsWith(".")
            || name in excludedFileNames
            || (relativeDir.isEmpty() && name in excludedRootEntries)
        ) return@forEach
        val relative = if (relativeDir.isEmpty()) name else "$relativeDir/$name"
        if (SystemFileSystem.metadataOrNull(child)?.isDirectory == true) {
            collectSiteFiles(relative, paths)
        } else {
            paths += relative
        }
    }
    return paths
}

private suspend fun renderPage(markdownFile: Path, htmlFile: Path, page: Page) {
    htmlFile.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(htmlFile).buffered().use { sink ->
        markdownFile
            .readLines()
            .flowOn(Dispatchers.IO)
            .renderMarkdownToHtml(page)
            .flowOn(Dispatchers.Default)
            .onEach { sink.writeString(it) }
            .flowOn(Dispatchers.IO)
            .collect()
    }
}

private fun copyFile(sourceFile: Path, targetFile: Path) {
    targetFile.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.source(sourceFile).buffered().use { source ->
        SystemFileSystem.sink(targetFile).use { sink ->
            source.transferTo(sink)
        }
    }
}

/**
 * Writes the Markdown [sourceFile] to [targetFile] [decorateMarkdown]d with
 * a provenance front matter for [pageUrl] and the copyright footer. The live
 * server decorates a served `.md` the same way, so the published file and the
 * preview differ only in the [pageUrl] host.
 */
private suspend fun copyMarkdown(sourceFile: Path, targetFile: Path, pageUrl: String) {
    targetFile.parent?.let { SystemFileSystem.createDirectories(it) }
    val content = SystemFileSystem.source(sourceFile).buffered().use { it.readString() }
    SystemFileSystem.sink(targetFile).buffered().use { sink ->
        sink.writeString(decorateMarkdown(content, pageUrl))
    }
}

/**
 * Reads this file as a cold [Flow] of lines, each terminated with `\n`.
 */
internal fun Path.readLines(): Flow<String> = flow {
    SystemFileSystem.source(this@readLines).buffered().use { source ->
        while (true) {
            val line = source.readLine() ?: break
            emit(line + "\n")
        }
    }
}
