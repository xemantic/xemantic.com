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

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Serves the website live from the project source directory, rendering
 * every Markdown page on each request so edits are visible on reload
 * without a build. Request paths resolve exactly as GitHub Pages resolves
 * the build output, so the preview matches what will be published:
 *
 * - `/foo/` renders `/foo/index.md` (or serves a static `/foo/index.html`),
 * - `/bar` renders `/bar.md` (or serves a static `/bar.html`), and
 *   redirects to `/bar/` when `/bar` is a directory holding an index,
 * - `/bar.md` serves the verbatim `/bar.md` source (an `/foo.md` with no
 *   direct source falls back to `/foo/index.md`, matching the flat alias
 *   the build emits),
 * - every other file (assets, legacy `.html`, …) is served verbatim,
 * - `/llms.txt`, which the build generates, is generated on each request,
 * - the repository infrastructure and hidden files are hidden, exactly as
 *   the build excludes them,
 * - a missing path responds `404`, with the site's custom root `404` page
 *   (rendered `404.md` or static `404.html`) as the body when present —
 *   note the `404/` directory is the *404* show, an ordinary page, not the
 *   error page.
 *
 * The port is taken from the `PORT` env variable, defaulting to `8080`.
 *
 * @param args optional website source directory (defaults to the current
 *   directory), followed by the optional directories of generated files,
 *   like the BeerCSS stylesheet, served as if they were a part of the source
 *   directory.
 */
fun main(args: Array<String>) {
    val root = Path(args.getOrElse(0) { "." })
    val generatedDirs = args.drop(1).map { Path(it) }
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val baseUrl = "http://localhost:$port"
    println("Serving $root live at $baseUrl")
    embeddedServer(CIO, port = port) {
        website(root, baseUrl, generatedDirs)
    }.start(wait = true)
}

/**
 * Installs the live website routing under this application: a single
 * catch-all route resolves every request path against [root] to the file
 * the build would publish for that URL, then responds accordingly — a
 * rendered Markdown page, a verbatim file, a redirect, or a `404`. Served
 * Markdown gets a provenance front matter under [baseUrl], mirroring the
 * build (which uses the production host instead). Files missing in [root]
 * are looked up in the [generatedDirs].
 */
fun Application.website(
    root: Path,
    baseUrl: String,
    generatedDirs: List<Path> = emptyList()
) {
    routing {
        get("{path...}") {
            when (val resolution = root.resolve(call.request.path(), generatedDirs)) {
                is Resolution.Serve -> call.respondFile(resolution.file, baseUrl)
                is Resolution.Render -> call.respondRendered(resolution.markdown, resolution.page(root, baseUrl, generatedDirs))
                is Resolution.Redirect -> call.respondRedirect(resolution.location, permanent = false)
                Resolution.LlmsTxt -> call.respondText(
                    llmsTxt(baseUrl, root, generatedDirs),
                    ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                )
                Resolution.NotFound -> call.respondNotFound(root, baseUrl, generatedDirs)
            }
        }
    }
}

private sealed interface Resolution {
    /** A file served verbatim (asset, `.md` source, or legacy `.html`). */
    data class Serve(val file: Path) : Resolution
    /**
     * A Markdown source rendered to HTML on the fly, [path] being
     * relative to the root.
     */
    data class Render(val markdown: Path, val path: String) : Resolution {
        fun page(root: Path, baseUrl: String, generatedDirs: List<Path>) =
            Page(path, baseUrl, listOf(root) + generatedDirs)
    }
    data class Redirect(val location: String) : Resolution
    /** The `llms.txt` index, generated as the build generates it. */
    data object LlmsTxt : Resolution
    data object NotFound : Resolution
}

/**
 * Resolves a request path against this source root, mapping it to the
 * file the build would have published for the same URL: a Markdown source
 * to render, a file to serve verbatim, a redirect, or nothing.
 */
private fun Path.resolve(
    requestPath: String,
    generatedDirs: List<Path>
): Resolution {
    val relative = requestPath.trim('/')
    if (!relative.isServableSitePath()) return Resolution.NotFound
    if (requestPath.endsWith("/")) {
        indexChild(relative, "index.md")?.let {
            return Resolution.Render(it, relative.indexPath("index.md"))
        }
        indexChild(relative, "index.html")?.let { return Resolution.Serve(it) }
        return Resolution.NotFound
    }
    (listOf(this) + generatedDirs).firstNotNullOfOrNull { it.child(relative) }
        ?.let { return Resolution.Serve(it) }
    if (relative == "llms.txt") return Resolution.LlmsTxt
    // `foo.md` with no direct source falls back to `foo/index.md`, the flat
    // alias the build emits so a page URL plus `.md` yields its source
    if (relative.endsWith(".md")) {
        child("${relative.removeSuffix(".md")}/index.md")
            ?.let { return Resolution.Serve(it) }
    }
    child("$relative.md")?.let { return Resolution.Render(it, "$relative.md") }
    child("$relative.html")?.let { return Resolution.Serve(it) }
    // a bare path pointing at a directory with an index redirects to the
    // trailing-slash form, exactly like GitHub Pages
    if (child("$relative/index.md") != null || child("$relative/index.html") != null)
        return Resolution.Redirect("/$relative/")
    return Resolution.NotFound
}

/**
 * The custom body served for a missing path: the site's root `404` page,
 * rendered from `404.md` or served from a static `404.html`, following the
 * GitHub Pages convention. Absent one, there is no body. The `404/`
 * directory is deliberately *not* consulted: it holds the *404* show, an
 * ordinary page, not the error page.
 */
private fun Path.resolveNotFound(): Resolution =
    child("404.md")?.let { Resolution.Render(it, "404.md") }
        ?: child("404.html")?.let { Resolution.Serve(it) }
        ?: Resolution.NotFound

/**
 * The child at [relative] under this root, or `null` when it is not a
 * regular file. An empty [relative] (the root itself) is never a file.
 */
private fun Path.child(relative: String): Path? {
    if (relative.isEmpty()) return null
    val path = Path(this, relative)
    return path.takeIf { it.isFile() }
}

/**
 * The `index.*` child of the directory addressed by [relative], handling
 * the root, where [relative] is empty.
 */
private fun Path.indexChild(relative: String, index: String): Path? =
    child(relative.indexPath(index))

private fun String.indexPath(index: String): String =
    if (isEmpty()) index else "$this/$index"

/**
 * Whether this relative request path may be served: the build excludes
 * hidden files at every level, the repository infrastructure at the root,
 * and the repository's own docs anywhere, so the preview hides them too.
 */
private fun String.isServableSitePath(): Boolean {
    if (isEmpty()) return true
    val segments = split("/")
    return segments.none { it.startsWith(".") }
        && segments.first() !in excludedRootEntries
        && segments.last() !in excludedFileNames
}

private val htmlContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)

/**
 * Serves [file], its content type derived from the extension. A Markdown
 * source is [decorateMarkdown]d — a provenance front matter for its page
 * URL under [baseUrl], plus the copyright footer — exactly as the build
 * decorates the published `.md`; every other file is byte-for-byte verbatim.
 */
private suspend fun ApplicationCall.respondFile(
    file: Path,
    baseUrl: String,
    status: HttpStatusCode? = null
) {
    val body =
        if (file.name.endsWith(".md")) {
            val pageUrl = baseUrl + request.path().trimStart('/').markdownPagePath()
            decorateMarkdown(file.readBytes().decodeToString(), pageUrl).encodeToByteArray()
        } else {
            file.readBytes()
        }
    respondBytes(body, file.contentType(), status)
}

/**
 * Responds `404` with the site's custom root `404` body — rendered `404.md`
 * or static `404.html` — or an empty body when the site defines none. See
 * [Path.resolveNotFound].
 */
private suspend fun ApplicationCall.respondNotFound(
    root: Path,
    baseUrl: String,
    generatedDirs: List<Path>
) {
    val status = HttpStatusCode.NotFound
    when (val body = root.resolveNotFound()) {
        is Resolution.Render -> respondRendered(body.markdown, body.page(root, baseUrl, generatedDirs), status)
        is Resolution.Serve -> respondFile(body.file, baseUrl, status)
        else -> respondBytes(
            bytes = ByteArray(0),
            contentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
            status = status
        )
    }
}

/**
 * Streams [markdown], the source of the [page], to the response as it
 * renders, chunk by chunk, rather than buffering the whole document first. markanywhere renders
 * incrementally, so the first HTML reaches the browser while the tail of
 * the page is still being parsed. Reuses the exact pipeline the build uses,
 * so the streamed bytes match the built file. Writes to the multiplatform
 * [io.ktor.utils.io.ByteWriteChannel], which encodes each chunk as UTF-8
 * and flushes it out as it arrives.
 */
private suspend fun ApplicationCall.respondRendered(
    markdown: Path,
    page: Page,
    status: HttpStatusCode? = null
) {
    respondBytesWriter(htmlContentType, status) {
        markdown.readLines()
            .flowOn(Dispatchers.IO)
            .renderMarkdownToHtml(page)
            .flowOn(Dispatchers.Default)
            .onEach { writeString(it) }
            .collect()
    }
}

private fun Path.isFile(): Boolean =
    SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

private fun Path.readBytes(): ByteArray =
    SystemFileSystem.source(this).buffered().use { it.readByteArray() }

/** The RFC 7763 media type for the verbatim Markdown sources. */
private val markdownContentType = ContentType("text", "markdown").withCharset(Charsets.UTF_8)

/**
 * The content type for this file, derived from its extension. Markdown
 * sources get the RFC 7763 `text/markdown` type, which Ktor's mime
 * database does not know; every other type comes from Ktor, which since
 * 3.x already appends `charset=UTF-8` to textual types on its own.
 */
private fun Path.contentType(): ContentType =
    if (name.endsWith(".md")) markdownContentType
    else ContentType.defaultForFilePath(name)
