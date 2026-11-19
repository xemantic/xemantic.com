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

import java.net.URI

/** The destination of an inline Markdown link or image: `[text](destination "title")`. */
private val INLINE_LINK = Regex("""(]\(\s*)(<[^>\n]*>|[^)\s]+)""")

/** A link reference definition: `[label]: destination`. */
private val REFERENCE_DEFINITION = Regex("""^( {0,3}\[[^]]+]:\s*)(<[^>\n]*>|\S+)""")

/** A URL attribute of raw HTML embedded in the Markdown. */
private val HTML_URL_ATTRIBUTE = Regex("""(\b(?:href|src|poster)=")([^"]*)""")

/** A code span, its backtick run matched by the same run closing it. */
private val CODE_SPAN = Regex("""(`+).*?\1""")

/** The opening, or closing, fence of a fenced code block. */
private val CODE_FENCE = Regex("""^ {0,3}(`{3,}|~{3,})""")

/** A URL scheme, like `https:` or `mailto:`, making a URL absolute. */
private val URL_SCHEME = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:""")

/**
 * Rewrites the relative URLs of this Markdown body into absolute ones,
 * resolved against the [documentUrl] the source is relative to, so that the
 * published document stays self-contained wherever it is read from — a
 * flat alias like `/ai/workshops.md` sits one directory above its source,
 * and an LLM crawler may quote or cache the document anywhere else.
 * A root-relative URL gets the [siteUrl], an in-page `#fragment` is kept.
 *
 * Only the URLs are touched, the rest of each line stays verbatim: the
 * destinations of inline links and images, of link reference definitions,
 * and the `href`, `src` and `poster` attributes of raw HTML, while code
 * blocks and code spans are left alone.
 */
internal fun String.absolutizeLinks(documentUrl: String, siteUrl: String): String {
    var fence: String? = null
    return split("\n").joinToString("\n") { line ->
        val fenceMarker = CODE_FENCE.find(line)?.groupValues?.get(1)
        when {
            fence != null -> {
                if (fenceMarker != null && fenceMarker[0] == fence!![0] && fenceMarker.length >= fence!!.length) {
                    fence = null
                }
                line
            }
            fenceMarker != null -> {
                fence = fenceMarker
                line
            }
            else -> line.outsideCodeSpans { it.absolutizeUrls(documentUrl, siteUrl) }
        }
    }
}

private fun String.outsideCodeSpans(transform: (String) -> String): String {
    val result = StringBuilder()
    var start = 0
    CODE_SPAN.findAll(this).forEach { span ->
        result.append(transform(substring(start, span.range.first))).append(span.value)
        start = span.range.last + 1
    }
    return result.append(transform(substring(start))).toString()
}

private fun String.absolutizeUrls(documentUrl: String, siteUrl: String): String =
    listOf(INLINE_LINK, REFERENCE_DEFINITION, HTML_URL_ATTRIBUTE).fold(this) { text, pattern ->
        pattern.replace(text) { match ->
            val (prefix, destination) = match.destructured
            val bracketed = destination.startsWith("<")
            val url = if (bracketed) destination.removeSurrounding("<", ">") else destination
            val absolute = url.toAbsoluteUrl(documentUrl, siteUrl)
            prefix + if (bracketed) "<$absolute>" else absolute
        }
    }

/**
 * This URL resolved against the [documentUrl], unless it is absolute already,
 * protocol-relative, or a `#fragment` of the document itself. Like a browser,
 * it drops the `..` segments climbing above the site root.
 */
internal fun String.toAbsoluteUrl(documentUrl: String, siteUrl: String): String = when {
    isEmpty() || startsWith("#") || startsWith("//") || URL_SCHEME.containsMatchIn(this) -> this
    startsWith("/") -> siteUrl + this
    else -> {
        val resolved = try {
            URI(documentUrl).resolve(this)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Cannot resolve the link $this against $documentUrl", e)
        }
        var path = resolved.rawPath
        while (path.startsWith("/../")) path = path.removePrefix("/..")
        siteUrl + path +
            (resolved.rawQuery?.let { "?$it" } ?: "") +
            (resolved.rawFragment?.let { "#$it" } ?: "")
    }
}
