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
import com.xemantic.markanywhere.flow.SemanticEventScope
import com.xemantic.markanywhere.flow.semanticEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import java.net.URI

/**
 * The language of a page without a `lang` entry in its front matter.
 */
internal const val DEFAULT_LANG = "en"

private const val SITE_NAME = "Xemantic"

/**
 * A Markdown page being rendered.
 *
 * @param path the Markdown source path relative to the website root,
 *   like `ai/workshops/index.md`.
 * @param siteUrl the origin the site is published under, without a
 *   trailing slash, like `https://xemantic.com`.
 * @param siteRoots the directories holding the site files, the source
 *   directory first, followed by the directories of generated files.
 */
class Page(
    val path: String,
    val siteUrl: String,
    val siteRoots: List<Path>
) {

    /** The absolute URL the rendered page is published at. */
    val url: String = siteUrl + path.htmlPagePath()

    /** The absolute URL of the published Markdown source of this page. */
    val markdownUrl: String = "$siteUrl/${path.toFlatMarkdownAlias() ?: path}"

    /**
     * Resolves a URL given in the front matter: an absolute URL is kept,
     * a `/`-prefixed one is relative to the [siteUrl], and any other one
     * is relative to the page [url], like a link in the Markdown body.
     */
    fun resolveUrl(reference: String): String = when {
        "://" in reference -> reference
        reference.startsWith("/") -> siteUrl + reference
        else -> URI(url).resolve(reference).toString()
    }

    /**
     * The site file published at this absolute [url], or `null` when the
     * URL points outside the site. Fails when the site has no such file,
     * as it is a broken reference in the front matter.
     */
    fun siteFile(url: String): Path? {
        if (!url.startsWith("$siteUrl/")) return null
        val relative = url.removePrefix("$siteUrl/").substringBefore('?').substringBefore('#')
        return siteRoots
            .map { Path(it, relative) }
            .firstOrNull { SystemFileSystem.metadataOrNull(it)?.isRegularFile == true }
            ?: error("$path refers to $url, but there is no such file in the site")
    }

}

/**
 * Completes the `head` of the document, as emitted by `wrapInHtmlDocument`
 * from the front matter, with the metadata of the [page]:
 *
 * - `html` gets the [DEFAULT_LANG], unless the front matter sets its `lang`,
 * - the canonical URL of the page and the `alternate` link to its Markdown
 *   source, which the build publishes next to every page,
 * - the Open Graph properties shown in the link previews of social media
 *   and messengers, derived from the `title` and the `description`,
 * - the `image` entry, which `wrapInHtmlDocument` would leave as a
 *   meaningless `<meta name="image">`, becomes the `og:image` with an
 *   absolute URL (see [Page.resolveUrl]), together with its dimensions,
 *   letting the preview show on the first share, before a crawler has
 *   downloaded the image, and with the `twitter:card` asking X for the
 *   large image preview.
 */
fun Flow<SemanticEvent>.addPageMetadata(page: Page): Flow<SemanticEvent> = semanticEvents {
    var inHead = false
    var inTitle = false
    val title = StringBuilder()
    var description: String? = null
    var image: String? = null
    var skippedMeta = false
    collect { event ->
        when {
            event is SemanticEvent.Mark && event.name == "html" && event["lang"] == null ->
                emit(event.copy(attributes = event.attributes + ("lang" to DEFAULT_LANG)))
            !inHead -> {
                if (event is SemanticEvent.Mark && event.name == "head") inHead = true
                emit(event)
            }
            event is SemanticEvent.Mark && event.name == "meta" && event["name"] == "image" -> {
                image = event["content"]
                skippedMeta = true
            }
            event is SemanticEvent.Unmark && event.name == "meta" && skippedMeta -> {
                skippedMeta = false
            }
            event is SemanticEvent.Unmark && event.name == "head" -> {
                emitPageMetadata(page, title.toString(), description, image)
                emit(event)
                inHead = false
            }
            else -> {
                when (event) {
                    is SemanticEvent.Mark if event.name == "title" -> inTitle = true
                    is SemanticEvent.Unmark if event.name == "title" -> inTitle = false
                    is SemanticEvent.Text if inTitle -> title.append(event.text)
                    is SemanticEvent.Mark if event.name == "meta"
                            && event["name"] == "description" -> description = event["content"]
                    else -> {}
                }
                emit(event)
            }
        }
    }
}

private suspend fun SemanticEventScope.emitPageMetadata(
    page: Page,
    title: String,
    description: String?,
    image: String?
) {
    "link"("rel" to "canonical", "href" to page.url) {}
    "link"(
        "rel" to "alternate",
        "type" to "text/markdown",
        "href" to page.markdownUrl
    ) {}
    fun property(name: String, content: String) = mapOf("property" to name, "content" to content)
    "meta"(property("og:type", "website")) {}
    "meta"(property("og:site_name", SITE_NAME)) {}
    "meta"(property("og:title", title.trim())) {}
    description?.let { "meta"(property("og:description", it)) {} }
    "meta"(property("og:url", page.url)) {}
    image?.let { page.resolveUrl(it) }?.let { imageUrl ->
        "meta"(property("og:image", imageUrl)) {}
        page.siteFile(imageUrl)?.readImageSize()?.let { (width, height) ->
            "meta"(property("og:image:width", width.toString())) {}
            "meta"(property("og:image:height", height.toString())) {}
        }
        "meta"("name" to "twitter:card", "content" to "summary_large_image") {}
    }
}

/**
 * Reads the width and height of this PNG, JPEG, GIF or WebP image from
 * its header, without decoding it, or `null` for any other format.
 */
internal fun Path.readImageSize(): Pair<Int, Int>? {
    val bytes = SystemFileSystem.source(this).buffered().use { it.readByteArray() }
    fun u8(offset: Int) = bytes[offset].toInt() and 0xFF
    fun u16be(offset: Int) = (u8(offset) shl 8) or u8(offset + 1)
    fun u16le(offset: Int) = u8(offset) or (u8(offset + 1) shl 8)
    fun u24le(offset: Int) = u16le(offset) or (u8(offset + 2) shl 16)
    fun u32be(offset: Int) = (u16be(offset) shl 16) or u16be(offset + 2)
    fun ascii(offset: Int, length: Int) =
        if (bytes.size < offset + length) "" else bytes.decodeToString(offset, offset + length)
    return when {
        ascii(1, 3) == "PNG" -> u32be(16) to u32be(20)
        ascii(0, 3) == "GIF" -> u16le(6) to u16le(8)
        ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP" -> when (ascii(12, 4)) {
            "VP8 " -> (u16le(26) and 0x3FFF) to (u16le(28) and 0x3FFF)
            "VP8L" -> {
                val bits = u16le(21) or (u16le(23) shl 16)
                (1 + (bits and 0x3FFF)) to (1 + ((bits shr 14) and 0x3FFF))
            }
            "VP8X" -> (1 + u24le(24)) to (1 + u24le(27))
            else -> null
        }
        bytes.size > 2 && u8(0) == 0xFF && u8(1) == 0xD8 -> {
            // walk the JPEG segments up to the start of frame, holding the size
            var offset = 2
            while (offset + 9 < bytes.size && u8(offset) == 0xFF) {
                val marker = u8(offset + 1)
                if (marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    return u16be(offset + 7) to u16be(offset + 5)
                }
                offset += 2 + u16be(offset + 2)
            }
            null
        }
        else -> null
    }
}
