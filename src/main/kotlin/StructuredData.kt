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

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/*
 * The schema.org structured data of every page, emitted as JSON-LD, so that
 * search engines, and the AI answers built on them, learn who wrote a page,
 * when, about what, and who stands behind the site. The entities form one
 * graph across the site, connected by the stable `@id` of the organization
 * and of each person.
 */

/**
 * A person the content can name as its `author`, by the [key], and the
 * profile page of whom, the Markdown source at [profilePath], becomes a
 * schema.org `ProfilePage` of this person, its social media links (see
 * `ContentWidgets.kt`) becoming the `sameAs` of the person.
 */
internal class Person(
    val key: String,
    val name: String,
    val profilePath: String
) {
    fun url(siteUrl: String) = siteUrl + profilePath.htmlPagePath()
    fun id(siteUrl: String) = url(siteUrl) + "#person"
}

/** The people of the site, keyed by the `author` value of the front matter. */
internal val PEOPLE = listOf(
    Person(key = "kazik", name = "Kazik Pogoda", profilePath = "kazik/index.md")
).associateBy { it.key }

/** The founder of the organization, see [PEOPLE]. */
private const val FOUNDER = "kazik"

/** The profiles of the organization itself, as listed on its contact page. */
private val ORGANIZATION_SAME_AS = listOf(
    "https://github.com/xemantic",
    "https://bsky.app/profile/xemantic.com"
)

private const val ORGANIZATION_LOGO = "/assets/images/logo.svg"

/** The directory whose pages are blog posts. */
internal const val BLOG_DIR = "blog/"

/**
 * The schema.org type of a page, set by the `type` entry of its front matter,
 * or implied: a page in the [BLOG_DIR] is a [BlogPosting], the profile page of
 * a [Person] is a [ProfilePage], and any other page a [WebPage].
 */
internal enum class PageType(
    /** Whether the page is a creative work, requiring an author and a publication date. */
    val authored: Boolean = false
) {
    WebPage,
    Article(authored = true),
    BlogPosting(authored = true),
    ProfilePage
}

/**
 * The front matter entries describing a page as a creative work, besides
 * the `title`, `description` and `image` handled by [addPageMetadata].
 */
internal val STRUCTURED_DATA_KEYS = setOf("type", "author", "datePublished", "dateModified", "about")

/**
 * The validated metadata of a page, given its front matter [entries] (only
 * the [STRUCTURED_DATA_KEYS] and `description` are consulted). An invalid
 * front matter fails the build, naming the page and how to fix it.
 */
internal class PageInfo(private val page: Page, entries: Map<String, String>) {

    private fun fail(message: String): Nothing = error("${page.path}: $message")

    val profileOf: Person? = PEOPLE.values.firstOrNull { it.profilePath == page.path }

    val type: PageType = run {
        val implied = when {
            page.path.startsWith(BLOG_DIR) -> PageType.BlogPosting
            profileOf != null -> PageType.ProfilePage
            else -> PageType.WebPage
        }
        val declared = entries["type"]?.let { name ->
            PageType.entries.firstOrNull { it.name == name }
                ?: fail("unknown type: $name, expected one of ${PageType.entries.joinToString()}")
        }
        when {
            declared == null || declared == implied -> implied
            implied == PageType.WebPage && declared == PageType.Article -> declared
            else -> fail(
                "the type $declared does not apply here, a page is a ${PageType.BlogPosting} " +
                    "exactly when it is in $BLOG_DIR, and a ${PageType.ProfilePage} exactly when " +
                    "it is the profile page of a person in PEOPLE"
            )
        }
    }

    val author: Person? = entries["author"]?.let { key ->
        PEOPLE[key] ?: fail("unknown author: $key, expected one of ${PEOPLE.keys.joinToString()}")
    }

    val datePublished: String? = entries["datePublished"]?.also { it.checkDate("datePublished") }

    val dateModified: String? = entries["dateModified"]?.also { it.checkDate("dateModified") }

    val about: String? = entries["about"]?.also {
        if (!it.startsWith("https://")) fail("about should be the https URL of the concept, like its Wikidata entry, not: $it")
    }

    init {
        if (type.authored) {
            val missing = listOf(
                "author" to author,
                "datePublished" to datePublished,
                "description" to entries["description"]
            ).filter { it.second == null }.map { it.first }
            if (missing.isNotEmpty()) fail("the type $type requires ${missing.joinToString()} in its front matter")
        }
        if (datePublished != null && dateModified != null && dateModified.take(10) < datePublished.take(10)) {
            fail("dateModified $dateModified precedes datePublished $datePublished")
        }
    }

    private fun String.checkDate(key: String) {
        try {
            if (length == 10) LocalDate.parse(this) else OffsetDateTime.parse(this)
        } catch (e: DateTimeParseException) {
            fail("$key should be a date like 2026-11-20, or a date-time with an offset, not: $this")
        }
    }

}

/**
 * The JSON-LD graph of the [page]: the organization publishing the site, the
 * site itself, and the page as its [PageInfo.type], titled [title], with the
 * absolute [imageUrl] of its image, in the [lang]. A [PageType.ProfilePage]
 * also holds its person, depicted by the absolute [personImageUrl], with the
 * [sameAs] profiles.
 */
internal fun structuredData(
    page: Page,
    info: PageInfo,
    title: String,
    description: String?,
    imageUrl: String?,
    lang: String,
    personImageUrl: String?,
    sameAs: List<String>
): String {
    val siteUrl = page.siteUrl
    val organizationId = "$siteUrl/#organization"
    val websiteId = "$siteUrl/#website"
    fun personRef(person: Person) = mapOf(
        "@type" to "Person",
        "@id" to person.id(siteUrl),
        "name" to person.name,
        "url" to person.url(siteUrl)
    )
    val founder = PEOPLE.getValue(FOUNDER)
    val organization = mapOf(
        "@type" to "Organization",
        "@id" to organizationId,
        "name" to SITE_NAME,
        "url" to "$siteUrl/",
        "logo" to siteUrl + ORGANIZATION_LOGO,
        "sameAs" to ORGANIZATION_SAME_AS,
        "founder" to personRef(founder)
    )
    val website = mapOf(
        "@type" to "WebSite",
        "@id" to websiteId,
        "name" to SITE_NAME,
        "url" to "$siteUrl/",
        "publisher" to mapOf("@id" to organizationId)
    )
    val pageNode = buildMap {
        put("@type", info.type.name)
        put("@id", page.url + if (info.type.authored) "#article" else "")
        put(if (info.type.authored) "headline" else "name", title)
        description?.let { put("description", it) }
        imageUrl?.let { put("image", it) }
        put("inLanguage", lang)
        if (info.type.authored) {
            put("mainEntityOfPage", page.url)
            put("isPartOf", mapOf("@id" to websiteId))
            put("publisher", mapOf("@id" to organizationId))
        } else {
            put("url", page.url)
            put("isPartOf", mapOf("@id" to websiteId))
        }
        info.author?.let { put("author", personRef(it)) }
        info.datePublished?.let { put("datePublished", it) }
        info.dateModified?.let { put("dateModified", it) }
        info.about?.let { put("about", mapOf("@id" to it)) }
        info.profileOf?.let { person ->
            put("mainEntity", buildMap {
                putAll(personRef(person))
                personImageUrl?.let { put("image", it) }
                if (sameAs.isNotEmpty()) put("sameAs", sameAs)
                if (person.key == FOUNDER) put("worksFor", mapOf("@id" to organizationId))
            })
        }
    }
    return mapOf(
        "@context" to "https://schema.org",
        "@graph" to listOf(organization, website, pageNode)
    ).toJson()
}

/**
 * This value as JSON: a map, a list, a string, or `null`. Every `<` is
 * escaped, so that the JSON never closes the `script` element holding it.
 */
private fun Any?.toJson(): String = when (this) {
    null -> "null"
    is String -> buildString {
        append('"')
        this@toJson.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '<' || c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
    is Map<*, *> -> entries.joinToString(",", "{", "}") { (key, value) -> "${key.toString().toJson()}:${value.toJson()}" }
    is List<*> -> joinToString(",", "[", "]") { it.toJson() }
    else -> error("Not a JSON value: $this")
}
