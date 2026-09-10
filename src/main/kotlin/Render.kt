package com.xemantic.website

import com.xemantic.markanywhere.html.wrapInHtmlDocument
import com.xemantic.markanywhere.html.wrapInSections
import com.xemantic.markanywhere.parse.parse
import com.xemantic.markanywhere.render.asHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
 * YAML front matter into a `<head>` element. The document is prefixed
 * with the HTML5 doctype, otherwise browsers would fall back to the
 * quirks mode, where the BeerCSS body grid clamps `main` to the
 * viewport height, clipping the page content.
 */
fun Flow<String>.renderMarkdownToHtml(): Flow<String> = parse()
    .wrapInSections(tocDepth = 6)
    .repackageContentAndTocNav()
    .wrapInHtmlDocument()
    .applyPageLayout()
    .asHtml()
    .onStart { emit("<!DOCTYPE html>\n") }

/**
 * Root entries of the repository which are not a part of the website:
 * the build machinery and the Jekyll leftovers.
 */
internal val excludedRootEntries = setOf(
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
 * @param args the source directory (website root) and the target directory.
 */
fun main(args: Array<String>) {
    val sourceDir = Path(args[0])
    val targetDir = Path(args[1])
    val domain = sourceDir.readDomain()
    val baseUrl = "https://$domain"
    val markdownPaths = mutableListOf<String>()
    var pageCount = 0
    var assetCount = 0
    runBlocking(Dispatchers.Default) {
        sourceDir.collectSiteFiles().forEach { relativePath ->
            val sourceFile = Path(sourceDir, relativePath)
            if (relativePath.endsWith(".md")) {
                pageCount++
                val htmlPath = relativePath.removeSuffix(".md") + ".html"
                val pageUrl = baseUrl + relativePath.markdownPagePath()
                launch {
                    renderPage(sourceFile, Path(targetDir, htmlPath))
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
                markdownPaths += alias ?: relativePath
            } else {
                assetCount++
                launch(Dispatchers.IO) {
                    copyFile(sourceFile, Path(targetDir, relativePath))
                }
            }
        }
    }
    writeTextFile(Path(targetDir, ".nojekyll"), "")
    writeTextFile(Path(targetDir, "llms.txt"), llmsTxt(domain, markdownPaths))
    println("Rendered $pageCount pages and copied $assetCount assets to $targetDir")
}

/**
 * The flat Markdown alias of this relative path: `ai/index.md`
 * becomes `ai.md`, matching the `/ai` page URL with `.md` appended.
 * Non-index files already match this convention, and the root
 * `index.md` has no flat form, therefore `null`.
 */
private fun String.toFlatMarkdownAlias(): String? =
    if (endsWith("/index.md")) removeSuffix("/index.md") + ".md"
    else null

/**
 * Reads the website domain from the `CNAME` file.
 */
private fun Path.readDomain(): String =
    SystemFileSystem.source(Path(this, "CNAME")).buffered().use { source ->
        source.readLine()!!.trim()
    }

private fun llmsTxt(
    domain: String,
    markdownPaths: List<String>
): String = buildString {
    append("# $domain\n\n## Pages\n\n")
    markdownPaths.sorted().forEach { path ->
        append("- [${path.markdownPagePath()}](https://$domain/$path)\n")
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
 * Whether this Markdown document already opens with a YAML front matter,
 * i.e. its first line is the `---` delimiter.
 */
internal fun String.hasFrontMatter(): Boolean =
    lineSequence().firstOrNull()?.trimEnd() == "---"

/**
 * Decorates a Markdown document for publishing: its front matter is given a
 * `url` entry naming the [pageUrl] the document originates from — inserted
 * into an existing front matter, or added as a fresh one when the document
 * has none — and the site [MARKDOWN_COPYRIGHT_FOOTER] is appended. Serving
 * and building share this function, so the only intended difference between
 * the preview and the published file is the [pageUrl] host.
 */
internal fun decorateMarkdown(content: String, pageUrl: String): String {
    val document =
        if (content.hasFrontMatter()) content.withFrontMatterUrl(pageUrl)
        else "---\nurl: $pageUrl\n---\n\n$content"
    return document + MARKDOWN_COPYRIGHT_FOOTER
}

/**
 * Sets the `url` entry of this document's existing YAML front matter to
 * [pageUrl] as its first entry, dropping any previous `url`. A front matter
 * without a closing `---` is malformed and left untouched.
 */
private fun String.withFrontMatterUrl(pageUrl: String): String {
    val lines = split("\n")
    val closeOffset = lines.drop(1).indexOfFirst { it.trimEnd() == "---" }
    if (closeOffset == -1) return this
    val close = closeOffset + 1
    val entries = lines.subList(1, close).toMutableList()
    entries.removeAll { it.substringBefore(":").trim() == "url" }
    entries.add(0, "url: $pageUrl")
    return (listOf("---") + entries + "---" + lines.subList(close + 1, lines.size))
        .joinToString("\n")
}

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

private suspend fun renderPage(markdownFile: Path, htmlFile: Path) {
    htmlFile.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(htmlFile).buffered().use { sink ->
        markdownFile
            .readLines()
            .flowOn(Dispatchers.IO)
            .renderMarkdownToHtml()
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
private fun copyMarkdown(sourceFile: Path, targetFile: Path, pageUrl: String) {
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
