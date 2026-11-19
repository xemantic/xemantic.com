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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The class of a run of the text of an article, a column as wide
 * as a comfortable line of text, see [layOutArticle]. The background
 * shader calms down behind it, see `generative-art-deco.js`.
 */
private const val PROSE = "prose"

/** The class of an image of an article, set between the [PROSE] runs. */
private const val ARTICLE_IMAGE = "article-image"

/** The class of the images of an article directly following each other, laid out in columns. */
private const val ARTICLE_GALLERY = "article-gallery"

/** The blocks making up the text of an article. */
private val PROSE_BLOCKS = setOf(
    "p", "ul", "ol", "dl", "blockquote", "pre", "table", "address", "h2", "h3", "h4", "h5", "h6"
)

/**
 * The declared types of the pages laid out as an article, besides
 * the news posts: a `Course` page is as much a text to read.
 */
private val ARTICLE_LAYOUT_TYPES = setOf(PageType.Article, PageType.Course).map { it.name }

/**
 * Lays out the content of an article (a page of one of
 * the [ARTICLE_LAYOUT_TYPES], or a news post) for reading,
 * the Markdown source staying as it is:
 *
 * - the blocks of text following each other ([PROSE_BLOCKS]) are wrapped
 *   in a `div.prose`, a column limiting the length of their lines,
 * - an image, either a paragraph holding nothing else (but a link around
 *   the image), or a `figure` without a class, becomes
 *   a `figure.article-image`, set between
 *   the runs of text, up to the whole width of the page,
 * - the images directly following each other are wrapped in
 *   a `div.article-gallery`.
 *
 * Only the blocks at the top level of the page, and of the sections its
 * headings start, are considered, anything else, like the title or
 * a grid of cards, stays as it is, ending the run preceding it.
 *
 * It has to run after `wrapInHtmlDocument`, which moves the front matter
 * into the `head`, and before [addPageMetadata], which removes the `type`
 * of the page from there.
 */
fun Flow<SemanticEvent>.layOutArticle(page: Page): Flow<SemanticEvent> = flow {
    var article = page.isNewsPost
    var inBody = false
    // the top level block being read, and the nesting depth within it
    val block = mutableListOf<SemanticEvent>()
    var depth = 0
    // the end of the run of text being filled, null outside of one
    var proseEnd: SemanticEvent? = null
    // the images awaiting the block following them
    val images = mutableListOf<List<SemanticEvent>>()

    suspend fun endProse() {
        proseEnd?.let { emit(it) }
        proseEnd = null
    }

    suspend fun flushImages() {
        if (images.isEmpty()) return
        val gallery = images.size > 1
        val start = images.first().first() as SemanticEvent.Mark
        val end = images.first().last() as SemanticEvent.Unmark
        if (gallery) emit(start.copy(name = "div", attributes = mapOf("class" to ARTICLE_GALLERY)))
        images.forEach { image -> image.forEach { emit(it) } }
        if (gallery) emit(end.copy(name = "div"))
        images.clear()
    }

    suspend fun flushBlock() {
        val start = block.first() as SemanticEvent.Mark
        val end = block.last() as SemanticEvent.Unmark
        val content = block.subList(1, block.size - 1)
        val marks = content.filterIsInstance<SemanticEvent.Mark>()
        val imageOnly = start.name == "p" &&
            marks.map { it.name }.let { it == listOf("img") || it == listOf("a", "img") } &&
            content.filterIsInstance<SemanticEvent.Text>().all { it.text.isBlank() }
        val figure = start.name == "figure" && start["class"] == null
        when {
            imageOnly || figure -> {
                endProse()
                images += listOf(
                    start.copy(name = "figure", attributes = start.attributes + ("class" to ARTICLE_IMAGE))
                ) + content + end.copy(name = "figure")
            }
            start.name in PROSE_BLOCKS -> {
                flushImages()
                if (proseEnd == null) {
                    emit(start.copy(name = "div", attributes = mapOf("class" to PROSE)))
                    proseEnd = end.copy(name = "div")
                }
                block.forEach { emit(it) }
            }
            else -> {
                endProse()
                flushImages()
                block.forEach { emit(it) }
            }
        }
        block.clear()
    }

    collect { event ->
        when {
            !inBody -> {
                if (event is Mark) {
                    if (event.name == "meta" && event["name"] == "type" && event["content"] in ARTICLE_LAYOUT_TYPES) {
                        article = true
                    }
                    if (event.name == "body") inBody = article
                }
                emit(event)
            }
            depth > 0 -> {
                block += event
                if (event is Mark) depth++
                if (event is Unmark && --depth == 0) flushBlock()
            }
            // a section started by a heading, its blocks are laid out like the top level ones
            event is Mark && event.name == "section" && event["class"] == null -> {
                endProse()
                flushImages()
                emit(event)
            }
            event is Mark -> {
                block += event
                depth = 1
            }
            event is Unmark -> {
                endProse()
                flushImages()
                if (event.name == "body") inBody = false
                emit(event)
            }
            // the white space between the blocks
            else -> if (images.isEmpty()) emit(event)
        }
    }
}
