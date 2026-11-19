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

import com.xemantic.markanywhere.parse.parse
import kotlinx.io.files.Path

/**
 * The Markdown file, at the website root, listing the site map shown in
 * the footer of every page, and holding its colophon. It is not published itself, see
 * [excludedRootEntries].
 */
internal const val SITE_FOOTER_FILE = "footer.md"

/**
 * A link of the site footer, with the [note] following it in its list
 * item, if any, like the `(alpha)` of `- [Umwelt](https://…) (alpha)`.
 */
class FooterLink(val label: String, val href: String, val note: String? = null)

/**
 * A titled column of the site footer,
 * the title being a link if the heading holds one.
 */
class FooterGroup(val title: String, val href: String?, val links: List<FooterLink>)

/**
 * A piece of the colophon closing the site footer,
 * either plain text, or a link if it has a [href].
 */
class FooterText(val text: String, val href: String? = null)

/**
 * The site footer, as authored in the [SITE_FOOTER_FILE]:
 * the columns of the site map, and the [colophon] below them, empty if there is none.
 */
class SiteFooter(val groups: List<FooterGroup>, val colophon: List<FooterText>)

/**
 * Reads the footer from the [SITE_FOOTER_FILE] in this
 * website root: every `h2` starts a [FooterGroup], linked if the heading
 * is a link, and every link following it, in a list, becomes
 * one of its [FooterLink]s, together with the text following it up to
 * the next link or the end of its list item.
 * A paragraph outside of any list is the colophon, its links kept in place.
 * Everything else is ignored. A link before the first `h2` fails, as it
 * would be silently lost otherwise.
 */
internal suspend fun Path.readSiteFooter(): SiteFooter {
    val file = Path(this, SITE_FOOTER_FILE)
    val groups = mutableListOf<FooterGroup>()
    var title: String? = null
    var titleHref: String? = null
    var links = mutableListOf<FooterLink>()
    var href: String? = null
    val text = StringBuilder()
    var inHeading = false
    // the link read already, still collecting the note following it
    var label: String? = null
    var labelHref: String? = null
    val colophon = mutableListOf<FooterText>()
    var listDepth = 0
    var inColophon = false
    var colophonHref: String? = null
    val colophonText = StringBuilder()
    fun closeColophonText() {
        if (colophonText.isNotEmpty()) colophon += FooterText(colophonText.toString(), colophonHref)
        colophonText.clear()
    }
    fun closeLink() {
        val linkLabel = label ?: return
        links += FooterLink(linkLabel, labelHref!!, text.toString().trim().ifEmpty { null })
        label = null
    }
    fun closeGroup() {
        closeLink()
        title?.let { groups += FooterGroup(it, titleHref, links) }
        titleHref = null
        links = mutableListOf()
    }
    file.readLines().parse().collect { event ->
        when (event) {
            is Mark -> when {
                event.name == "ul" || event.name == "ol" -> listDepth++
                event.name == "p" && listDepth == 0 -> {
                    // paragraphs are joined, should there be more than one
                    if (colophon.isNotEmpty()) colophon += FooterText(" ")
                    inColophon = true
                }
                event.name == "a" && inColophon -> {
                    closeColophonText()
                    colophonHref = event["href"]
                }
                event.name == "h2" -> {
                    closeGroup()
                    inHeading = true
                    text.clear()
                }
                event.name == "a" && inHeading -> titleHref = event["href"]
                event.name == "a" -> {
                    closeLink()
                    check(title != null) {
                        "$file: the link to ${event["href"]} precedes the first ## heading"
                    }
                    href = event["href"]
                    text.clear()
                }
            }
            is Text -> when {
                inColophon -> colophonText.append(event.text)
                inHeading || href != null || label != null -> text.append(event.text)
            }
            is Unmark -> when {
                event.name == "ul" || event.name == "ol" -> listDepth--
                event.name == "a" && inColophon -> {
                    closeColophonText()
                    colophonHref = null
                }
                event.name == "p" && inColophon -> {
                    closeColophonText()
                    inColophon = false
                }
                event.name == "h2" -> {
                    title = text.toString().trim()
                    inHeading = false
                }
                event.name == "a" && href != null -> {
                    label = text.toString().trim()
                    labelHref = href
                    href = null
                    text.clear()
                }
                event.name == "li" -> closeLink()
            }
        }
    }
    closeGroup()
    return SiteFooter(groups, colophon)
}
