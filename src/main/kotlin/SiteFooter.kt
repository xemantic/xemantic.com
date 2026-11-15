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
 * the footer of every page. It is not published itself, see
 * [excludedRootEntries].
 */
internal const val SITE_FOOTER_FILE = "footer.md"

/**
 * A link of the site footer.
 */
class FooterLink(val label: String, val href: String)

/**
 * A titled column of the site footer.
 */
class FooterGroup(val title: String, val links: List<FooterLink>)

/**
 * Reads the site map of the footer from the [SITE_FOOTER_FILE] in this
 * website root: every `h2` starts a [FooterGroup], and every link
 * following it, typically in a list, becomes one of its [FooterLink]s.
 * Everything else is ignored. A link before the first `h2` fails, as it
 * would be silently lost otherwise.
 */
internal suspend fun Path.readSiteFooter(): List<FooterGroup> {
    val file = Path(this, SITE_FOOTER_FILE)
    val groups = mutableListOf<FooterGroup>()
    var title: String? = null
    var links = mutableListOf<FooterLink>()
    var href: String? = null
    val text = StringBuilder()
    var inHeading = false
    fun closeGroup() {
        title?.let { groups += FooterGroup(it, links) }
        links = mutableListOf()
    }
    file.readLines().parse().collect { event ->
        when (event) {
            is Mark -> when {
                event.name == "h2" -> {
                    closeGroup()
                    inHeading = true
                    text.clear()
                }
                event.name == "a" && !inHeading -> {
                    check(title != null) {
                        "$file: the link to ${event["href"]} precedes the first ## heading"
                    }
                    href = event["href"]
                    text.clear()
                }
            }
            is Text -> if (inHeading || href != null) text.append(event.text)
            is Unmark -> when {
                event.name == "h2" -> {
                    title = text.toString().trim()
                    inHeading = false
                }
                event.name == "a" && href != null -> {
                    links += FooterLink(text.toString().trim(), href!!)
                    href = null
                }
            }
        }
    }
    closeGroup()
    return groups
}
