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
import com.xemantic.markanywhere.transform.transform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import java.net.URLEncoder

/**
 * The class of a `div` holding a Markdown list of links to social media
 * profiles, rendered as a `nav` holding a row of [brandIcon] buttons:
 *
 * ```markdown
 * <div class="social-media-links">
 *
 * - [X](https://x.com/kazikpogoda)
 * - [GitHub](https://github.com/morisil/)
 *
 * </div>
 * ```
 *
 * The blank lines matter: CommonMark ends an HTML block at a blank line,
 * so only then is the list parsed as Markdown.
 */
private const val SOCIAL_MEDIA_LINKS = "social-media-links"

/**
 * The class of a `figure` holding a Markdown link to a YouTube video,
 * rendered as the embedded player, the link text becoming its title:
 *
 * ```markdown
 * <figure class="youtube-video">
 *
 * [Video title](https://www.youtube.com/watch?v=rRjG86TAyq4)
 *
 * </figure>
 * ```
 */
private const val YOUTUBE_VIDEO = "youtube-video"

/**
 * The class of a `figure` holding a Markdown link to a Google Maps embed URL
 * (the `src` of the `iframe` Google Maps offers to embed), the link text naming
 * the place:
 *
 * ```markdown
 * <figure class="google-map">
 *
 * [Prachtsaal Studio, Jonasstraße 22, 12053 Berlin](https://www.google.com/maps/embed?pb=…)
 *
 * </figure>
 * ```
 *
 * The map is not embedded right away, as Google would set its cookies on
 * every visit: the figure holds a button loading it on demand, see
 * [GOOGLE_MAP_JS], and a link opening the place in Google Maps instead.
 */
private const val GOOGLE_MAP = "google-map"

/** The script replacing the consent panel of a [GOOGLE_MAP] with the map, once asked to. */
private const val GOOGLE_MAP_JS = "/assets/js/google-map.js"

private const val GOOGLE_MAP_EMBED_URL = "https://www.google.com/maps/embed?"

/**
 * The class of a `section` introducing a person: a paragraph holding
 * just the photograph, followed by the bio, any Markdown, typically
 * ending with the [SOCIAL_MEDIA_LINKS]:
 *
 * ```markdown
 * <section class="profile">
 *
 * ![Kazik Pogoda](../assets/images/people/kazik/photo.jpg)
 *
 * I am an independent AI researcher …
 *
 * </section>
 * ```
 *
 * The bio is rendered wrapped in a `div.profile-bio`, followed by the
 * photograph as `img.profile-photo`, so that `xemantic.css` can lay
 * them out as the two cells of a grid.
 */
private const val PROFILE = "profile"

/** The mode of the paragraph holding the photograph of a [PROFILE]. */
private const val PROFILE_PHOTO = "profile-photo"

/**
 * Prism, highlighting the fenced code blocks in the browser, generated
 * together with the grammars of the languages the site uses by
 * `./gradlew generatePrism`, see `build.gradle.kts`. It is loaded `async`,
 * and only by a page holding a code block: once it has run, at whatever
 * moment, it highlights every `code` of a `language-…` class on its own,
 * while the code stays readable, just not colored, until then.
 */
private const val PRISM_JS = "/assets/js/prism.js"

/**
 * The brand icons available in `xemantic.css`, as `brand-icon` modifier
 * classes, keyed by the host of the profile URL they stand for.
 */
private val BRAND_ICONS = mapOf(
    "github.com" to "github",
    "x.com" to "x",
    "twitter.com" to "x",
    "linkedin.com" to "linkedin",
    "instagram.com" to "instagram",
    "youtube.com" to "youtube",
    "bsky.app" to "bluesky"
)

/**
 * The `brand-icon` modifier class of the service this [href] points to.
 * An unknown service fails the build, as its link would render as an
 * empty button otherwise.
 */
internal fun brandIcon(href: String): String {
    val host = href.substringAfter("://").substringBefore('/').removePrefix("www.")
    return checkNotNull(BRAND_ICONS[host]) {
        "No brand icon for $href, add one to xemantic.css and BRAND_ICONS"
    }
}

private val YOUTUBE_VIDEO_ID = Regex(
    """^https://(?:www\.youtube\.com/watch\?(?:.*&)?v=|youtu\.be/)([\w-]+)"""
)

internal fun SemanticEvent.Mark.hasClass(cssClass: String) =
    this["class"]?.split(' ')?.contains(cssClass) == true

/**
 * Renders the Markdown widgets of the content, plain Markdown wrapped in an
 * HTML element of a semantic class, so that the source stays readable as
 * Markdown, while the rendered page gets the markup it needs, see
 * [SOCIAL_MEDIA_LINKS], [YOUTUBE_VIDEO], [GOOGLE_MAP] and [PROFILE]. Of the wrapped Markdown only
 * the links are kept, any surrounding list or paragraph is dropped,
 * except for the [PROFILE], which keeps its bio as it is. A fenced code block
 * is followed by [PRISM_JS], highlighting it.
 */
fun Flow<SemanticEvent>.applyContentWidgets(): Flow<SemanticEvent> = transform {

    // the text of the link being read, which the widget needs only at its end
    var label = StringBuilder()

    match({ name == "div" && hasClass(SOCIAL_MEDIA_LINKS) }) {
        "nav"(
            "class" to "$SOCIAL_MEDIA_LINKS wrap center-align",
            "aria-label" to "Social media"
        ) {
            children(mode = SOCIAL_MEDIA_LINKS)
        }
    }

    match("a", mode = SOCIAL_MEDIA_LINKS) { mark ->
        val href = checkNotNull(mark["href"]) { "A social media link without href" }
        val icon = brandIcon(href)
        label = StringBuilder()
        children(mode = "label")
        defer {
            val name = label.toString().trim()
            "a"(
                "href" to href,
                "class" to "circle transparent",
                "aria-label" to name,
                "title" to name
            ) {
                "span"("class" to "brand-icon $icon") {}
            }
        }
    }

    // the attributes of the photograph of the profile being read, null until its paragraph
    var profilePhoto: Map<String, String>? = null

    match({ name == "section" && hasClass(PROFILE) }) { mark ->
        profilePhoto = null
        "section"(mark.attributes) {
            "div"("class" to "profile-bio") {
                children(mode = PROFILE)
            }
            defer {
                val photo = checkNotNull(profilePhoto) {
                    "A profile not starting with a paragraph holding the photograph"
                }
                "img"(photo + ("class" to PROFILE_PHOTO)) {}
            }
        }
    }

    match("p", mode = PROFILE) { mark ->
        if (profilePhoto == null) {
            profilePhoto = emptyMap()
            children(mode = PROFILE_PHOTO)
        } else {
            "p"(mark.attributes) {
                children()
            }
        }
    }

    match("img", mode = PROFILE_PHOTO) { mark ->
        profilePhoto = mark.attributes
    }

    match("*", mode = PROFILE_PHOTO) { children(mode = PROFILE_PHOTO) }
    matchText(mode = PROFILE_PHOTO) { text ->
        check(text.isBlank()) {
            "The first paragraph of a profile should hold just the photograph, not: $text"
        }
    }

    match({ name == "figure" && hasClass(YOUTUBE_VIDEO) }) { mark ->
        "figure"(mark.attributes) {
            children(mode = YOUTUBE_VIDEO)
        }
    }

    match("a", mode = YOUTUBE_VIDEO) { mark ->
        val href = checkNotNull(mark["href"]) { "A YouTube video link without href" }
        val videoId = checkNotNull(YOUTUBE_VIDEO_ID.find(href)) {
            "Not a YouTube video URL: $href"
        }.groupValues[1]
        label = StringBuilder()
        children(mode = "label")
        defer {
            "iframe"(
                "src" to "https://www.youtube.com/embed/$videoId",
                "title" to label.toString().trim(),
                "allow" to "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share",
                "referrerpolicy" to "strict-origin-when-cross-origin",
                "allowfullscreen" to ""
            ) {}
        }
    }

    match({ name == "figure" && hasClass(GOOGLE_MAP) }) { mark ->
        "figure"(mark.attributes) {
            children(mode = GOOGLE_MAP)
        }
        // deferred, so that it runs only once the page is parsed, whichever map it serves
        "script"("src" to GOOGLE_MAP_JS, "defer" to "") {}
    }

    match("a", mode = GOOGLE_MAP) { mark ->
        val href = checkNotNull(mark["href"]) { "A Google map link without href" }
        check(href.startsWith(GOOGLE_MAP_EMBED_URL)) {
            "Not a Google Maps embed URL: $href, expected one starting with $GOOGLE_MAP_EMBED_URL"
        }
        label = StringBuilder()
        children(mode = "label")
        defer {
            val place = label.toString().trim()
            val search = "https://www.google.com/maps/search/?api=1&query=" +
                URLEncoder.encode(place, Charsets.UTF_8)
            "div"("class" to "google-map-consent") {
                "p" { +"The map is served by Google, which may set cookies once it is shown." }
                "nav"("class" to "wrap center-align") {
                    "button"(
                        "class" to "border",
                        "data-google-map" to href,
                        "data-title" to "Map of $place"
                    ) {
                        "span" { +"Show the map" }
                    }
                    "a"("href" to search, "class" to "button transparent") {
                        "span" { +"Open in Google Maps" }
                        "i" { +"north_east" }
                    }
                }
            }
        }
    }

    // only the first code block of a page brings the script
    var prismAdded = false

    match("pre") { mark ->
        mark.name(mark.attributes) {
            children()
        }
        if (!prismAdded) {
            prismAdded = true
            "script"("src" to PRISM_JS, "async" to "") {}
        }
    }

    // the list or paragraph around the links, dropped
    listOf(SOCIAL_MEDIA_LINKS, YOUTUBE_VIDEO, GOOGLE_MAP).forEach { mode ->
        match("*", mode = mode) { children(mode = mode) }
        matchText(mode = mode) {}
    }

    match("*", mode = "label") { children(mode = "label") }
    matchText(mode = "label") { label.append(it) }

    passthrough()

}

/**
 * The classes the content widgets recognize in the Markdown source,
 * see [applyContentWidgets].
 */
private val CONTENT_WIDGET_CLASSES = setOf(SOCIAL_MEDIA_LINKS, YOUTUBE_VIDEO, GOOGLE_MAP, PROFILE)

/** The stylesheets every page links, see [applyPageLayout]. */
private val PAGE_STYLESHEETS = listOf(BEER_CSS, XEMANTIC_CSS)

/** The prefix of the classes the Markdown parser adds itself, like `language-kotlin` on a fenced code block. */
private const val PARSER_CLASS_PREFIX = "language-"

/** A class selector, which, unlike a length like `.5rem`, never starts with a digit. */
private val CLASS_SELECTOR = Regex("""\.(-?[_a-zA-Z][\w-]*)""")

/** The comments, strings and `url()` values, which may hold a dot not starting a class. */
private val CSS_NON_SELECTORS = Regex("""/\*.*?\*/|"[^"]*"|'[^']*'|url\([^)]*\)""", RegexOption.DOT_MATCHES_ALL)

/**
 * Fails on a class in the content of the [page] which is neither one of the
 * [CONTENT_WIDGET_CLASSES] nor defined in the [PAGE_STYLESHEETS], as it is
 * either a misspelled widget, silently rendered as a plain element, or a
 * BeerCSS class whose module is missing from `generateBeerCss`, silently
 * rendered unstyled. The stylesheets are read for every page, so that the
 * live server reflects their changes right away.
 *
 * It has to run on the content alone, before the operators adding their own
 * classes, like [applyContentWidgets] and [applyPageLayout].
 */
fun Flow<SemanticEvent>.checkContentClasses(page: Page): Flow<SemanticEvent> = flow {
    val definedClasses = PAGE_STYLESHEETS.flatMapTo(CONTENT_WIDGET_CLASSES.toMutableSet()) { stylesheet ->
        val file = checkNotNull(page.siteFile(page.siteUrl + stylesheet))
        val css = SystemFileSystem.source(file).buffered().use { it.readString() }
        CLASS_SELECTOR.findAll(css.replace(CSS_NON_SELECTORS, " ")).map { it.groupValues[1] }
    }
    emitAll(onEach { event ->
        if (event is Mark) {
            event["class"]?.split(' ')?.filter { it.isNotEmpty() && it !in definedClasses && !it.startsWith(PARSER_CLASS_PREFIX) }?.forEach {
                error(
                    "${page.path} uses the class `$it` on `${event.name}`, which is neither a content widget, " +
                        "nor defined in ${PAGE_STYLESHEETS.joinToString()}, " +
                        "check its spelling, or add its BeerCSS module to generateBeerCss"
                )
            }
        }
    })
}
