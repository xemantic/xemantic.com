package com.xemantic.website

import com.xemantic.markanywhere.SemanticEvent
import com.xemantic.markanywhere.flow.semanticEvents
import com.xemantic.markanywhere.transform.transform
import kotlinx.coroutines.flow.Flow

private const val BEER_CSS_VERSION = "4.0.23"

private const val BEER_CSS =
    "https://cdn.jsdelivr.net/npm/beercss@$BEER_CSS_VERSION/dist/cdn/beer.min.css"
private const val BEER_JS =
    "https://cdn.jsdelivr.net/npm/beercss@$BEER_CSS_VERSION/dist/cdn/beer.min.js"

private const val XEMANTIC_CSS = "/assets/css/xemantic.css"

private const val GITHUB_URL = "https://github.com/xemantic"

private const val GITHUB_ICON =
    "/assets/images/github/GitHub_Invertocat_Black.svg"

/**
 * The site-wide copyright line. Kept brand-level on purpose: the precise
 * legal entity behind "Xemantic" is disclosed in the `/impressum/` page,
 * so this notice needs no change when that entity formalizes. The site
 * content is all rights reserved; the code stays open source in its own
 * repositories under their own licenses.
 */
internal const val COPYRIGHT = "© 2026 Xemantic · All rights reserved"

/**
 * The [COPYRIGHT] as a Markdown footer, appended to every verbatim `.md`
 * document that is served, separated from the content by a thematic break.
 * It carries no Impressum link, unlike the rendered pages' footer: a plain
 * source file is not the place for site navigation.
 */
internal const val MARKDOWN_COPYRIGHT_FOOTER = "\n\n---\n\n$COPYRIGHT\n"

/**
 * Applies the xemantic.com page layout: the `head` is completed with
 * `charset`/`viewport` metas, the BeerCSS assets (stylesheet, module
 * script, material dynamic colors) and the xemantic.com stylesheet,
 * the `body` content is prefixed with
 * a top app bar (`header` with `nav`), the `main` element (synthesized
 * by [wrapBodyContentInMain]) becomes BeerCSS's `main.responsive`
 * container, the table of contents `nav` is wrapped in an `aside`, and
 * the `body` is closed with a `footer` carrying the [COPYRIGHT] notice
 * and a link to the Impressum.
 */
fun Flow<SemanticEvent>.applyPageLayout() = transform {

    match("head") {
        "head" {
            "meta"("charset" to "utf-8") {}
            "meta"(
                "name" to "viewport",
                "content" to "width=device-width, initial-scale=1"
            ) {}
            children()
            "link"("href" to BEER_CSS, "rel" to "stylesheet") {}
            "link"("href" to XEMANTIC_CSS, "rel" to "stylesheet") {}
            "script"("type" to "module", "src" to BEER_JS) {}
        }
    }

    match("body") {
        "body" {
            "header"("class" to "fixed") {
                "nav" {
                    "a"("href" to "/", "class" to "circle transparent") {
                        "i" { +"home" }
                    }
                    "h5"("class" to "max") { +"xemantic" }
                    "a"(
                        "href" to GITHUB_URL,
                        "class" to "circle transparent",
                        "aria-label" to "Xemantic on GitHub"
                    ) {
                        "img"(
                            "class" to "github-icon",
                            "src" to GITHUB_ICON,
                            "alt" to "GitHub"
                        ) {}
                    }
                }
            }
            children()
            "footer" {
                "nav" {
                    "span"("class" to "max") { +COPYRIGHT }
                    "a"("href" to "/impressum/") { +"Legal Notice (Impressum)" }
                }
            }
        }
    }

    match("main") {
        "main"("class" to "responsive") {
            children()
        }
    }

    passthrough()

}

/**
 * Wraps the direct content of `body` in a `main` element, except a trailing
 * `nav` with `id="toc"` (emitted at the very end of the stream by
 * `wrapInSections`), which stays a sibling of `main`. On a page without
 * a table of contents `main` simply closes together with `body`.
 */
fun Flow<SemanticEvent>.wrapBodyContentInMain(): Flow<SemanticEvent> = semanticEvents {
    var mainOpen = false
    collect { event ->
        when {
            event is SemanticEvent.Mark && event.name == "body" -> {
                emit(event)
                mark("main")
                mainOpen = true
            }
            mainOpen && event is SemanticEvent.Mark
                && event.name == "nav" && event["id"] == "toc" -> {
                unmark("main")
                mainOpen = false
                emit(event)
            }
            event is SemanticEvent.Unmark && event.name == "body" -> {
                if (mainOpen) {
                    unmark("main")
                    mainOpen = false
                }
                emit(event)
            }
            else -> emit(event)
        }
    }
}


fun Flow<SemanticEvent>.repackageContentAndTocNav() = semanticEvents {
    mark("main")
    var lastName = "main"
    collect { event ->
        if (event is SemanticEvent.Mark && event.name == "nav" && event["id"] == "toc") {
            lastName = "aside"
            unmark("main")
            mark("aside")
        }
        emit(event)
    }
    unmark(lastName)
}
