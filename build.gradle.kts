import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.version.catalog.update)
    application
}

val kotlinTarget = KotlinVersion.fromVersion(libs.versions.kotlinTarget.get())
val javaTarget = libs.versions.javaTarget.get()

kotlin {
    compilerOptions {
        apiVersion = kotlinTarget
        languageVersion = kotlinTarget
        jvmTarget = JvmTarget.fromTarget(javaTarget)
        freeCompilerArgs.addAll(
            "-Xjdk-release=$javaTarget",
            "-Xcontext-sensitive-resolution"
        )
        extraWarnings = true
        progressiveMode = true
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

versionCatalogUpdate {
    sortByKey = false
    keep {
        // plain version constants with no version.ref
        versions = setOf("kotlinTarget", "javaTarget", "beerCss", "prism")
        keepUnusedVersions = false
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.io.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.markanywhere.parse)
    implementation(libs.markanywhere.render)
    implementation(libs.markanywhere.transform)
    implementation(libs.markanywhere.html)
    implementation(libs.markanywhere.yaml)
    implementation(libs.xemantic.kotlin.core)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass = "com.xemantic.website.RenderKt"
}

/** The generated files published next to the website source. */
val generatedWebsiteDir = layout.buildDirectory.dir("generated/website")

/**
 * The origin the website is published under, used for the absolute URLs
 * of the pages, like the canonical and the Open Graph ones. Defaults to
 * the `CNAME` domain, a preview deployment overrides it with
 * `-PsiteUrl=https://…` (or the `ORG_GRADLE_PROJECT_siteUrl` env variable).
 */
val siteUrl = providers.gradleProperty("siteUrl").orElse(
    providers.fileContents(layout.projectDirectory.file("CNAME")).asText.map {
        "https://${it.trim()}"
    }
)

val websiteDir = layout.buildDirectory.dir("website")

tasks.run.configure {
    description = "Renders the whole website into the build folder."
    dependsOn("generateBeerCss", "generateArsenal", "generateUbuntuMono", "generatePrism")
    // a clean slate, so a deleted or renamed page does not linger
    // in the output, nor in the checks run on it
    val outputDir = websiteDir.get().asFile
    doFirst { outputDir.deleteRecursively() }
    args(
        layout.projectDirectory.asFile.absolutePath,
        outputDir.absolutePath,
        siteUrl.get(),
        generatedWebsiteDir.get().asFile.absolutePath
    )
}

tasks.register<JavaExec>("serve") {
    description = "Serves the website live from the source, rendering Markdown on each request."
    group = ApplicationPlugin.APPLICATION_GROUP
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.xemantic.website.ServerKt"
    dependsOn("generateBeerCss", "generateArsenal", "generateUbuntuMono", "generatePrism")
    args(
        layout.projectDirectory.asFile.absolutePath,
        generatedWebsiteDir.get().asFile.absolutePath
    )
}

/*
 * BeerCSS and Material Symbols are generated into the build and published
 * from the site's own origin, without any third-party request.
 * The task reruns only when the version (`beerCss` in `libs.versions.toml`),
 * the modules or the icons change,
 * otherwise the files are up to date and no download happens.
 */

/**
 * The BeerCSS modules concatenated into `assets/css/beercss.css`:
 * settings, then helpers, then elements, alphabetical within a group,
 * as the BeerCSS docs mandate. `settings/font` is left out on purpose,
 * its `@font-face` rules would pull in the full icon fonts.
 */
val beerCssModules = listOf(
    "settings/dark",
    "settings/global",
    "settings/light",
    "settings/reset",
    "settings/theme",
    "helpers/alignment",
    "helpers/form",
    "helpers/padding",
    "helpers/responsive",
    "helpers/wave",
    "elements/bar",
    "elements/button",
    "elements/card",
    "elements/dialog",
    "elements/divider",
    "elements/expansion",
    "elements/field",
    "elements/grid",
    "elements/icon",
    "elements/layout",
    "elements/list",
    "elements/mainLayout",
    "elements/menu",
    "elements/navigation",
    "elements/overlay",
    "elements/table",
    "elements/typography"
)

/**
 * The Material Symbols (Outlined) icons used on the site, written as
 * `<i>icon_name</i>`. Only these glyphs end up in the vendored font.
 * `checkMaterialSymbols`, run after every `./gradlew run`, fails the build
 * when a rendered page uses an icon missing here.
 */
val materialSymbols = listOf(
    "architecture",
    "arrow_forward",
    "balance",
    "check",
    "code",
    "content_copy",
    "dark_mode",
    "diversity_3",
    "engineering",
    "expand_more",
    "favorite",
    "handshake",
    "inventory_2",
    "light_mode",
    "mail",
    "menu",
    "menu_open",
    "newspaper",
    "north_east",
    "palette",
    "person",
    "psychology",
    "school",
    "science",
    "slideshow",
    "smart_toy",
    "toc",
    "trending_up"
)

/** A task downloading the files it generates from the web. */
abstract class DownloadTask : DefaultTask() {

    protected fun get(url: String): ByteArray {
        // not a field, as the configuration cache cannot store a client
        val http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val request = HttpRequest.newBuilder(URI(url))
            // Google Fonts serves woff2 only to a browser it recognizes
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        check(response.statusCode() == 200) {
            "GET $url responded with ${response.statusCode()}"
        }
        return response.body()
    }

}

/**
 * Downloads the [modules] of BeerCSS [version] and concatenates them into
 * [css], followed by a `@font-face` for the [icons] subset of the
 * Material Symbols Outlined font, which is downloaded into [font].
 */
@CacheableTask
abstract class GenerateBeerCss : DownloadTask() {

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val modules: ListProperty<String>

    @get:Input
    abstract val icons: ListProperty<String>

    @get:OutputFile
    abstract val css: RegularFileProperty

    @get:OutputFile
    abstract val font: RegularFileProperty

    @TaskAction
    fun update() {
        val cdn = "https://cdn.jsdelivr.net/npm/beercss@${version.get()}/dist/cdn"
        val iconNames = icons.get().sorted().joinToString(",")
        val fontCss = get(
            // FILL for BeerCSS's filled icons, GRAD for the hover effect in xemantic.css
            "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:FILL,GRAD@0..1,-25..200&icon_names=$iconNames"
        ).decodeToString()
        val fontUrl = Regex("""src: url\((https://[^)]+)\) format\('woff2'\)""")
            .find(fontCss)?.groupValues?.get(1)
            ?: error("No woff2 font in the Google Fonts response:\n$fontCss")
        font.get().asFile.writeBytes(get(fontUrl))
        val fontPath = font.get().asFile.relativeTo(css.get().asFile.parentFile).invariantSeparatorsPath
        css.get().asFile.writeText(buildString {
            append("/*\n")
            append(" * Generated by `./gradlew generateBeerCss`, do not edit.\n")
            append(" * BeerCSS ${version.get()} (MIT License, https://github.com/beercss/beercss),\n")
            append(" * modules: ${modules.get().joinToString()}.\n")
            append(" * Material Symbols Outlined (Apache License 2.0, https://github.com/google/material-design-icons),\n")
            append(" * icons: ${icons.get().sorted().joinToString()}.\n")
            append(" */\n")
            modules.get().forEach { module ->
                append(get("$cdn/$module.min.css").decodeToString().trim())
                append("\n")
            }
            append("@font-face{font-family:\"Material Symbols Outlined\";font-style:normal;font-weight:400;")
            append("font-display:block;src:url($fontPath) format(\"woff2\")}\n")
        })
    }

}

tasks.register<GenerateBeerCss>("generateBeerCss") {
    description = "Generates the BeerCSS modules and the Material Symbols subset used by the website."
    group = "website"
    version = libs.versions.beerCss
    modules = beerCssModules
    icons = materialSymbols
    css = generatedWebsiteDir.map { it.file("assets/css/beercss.css") }
    font = generatedWebsiteDir.map { it.file("assets/fonts/material-symbols-outlined.woff2") }
}

/**
 * Downloads the static [family] font from Google Fonts, in the [styles]
 * (`ital,wght` tuples, like `1,700` for bold italic) and the [subsets],
 * into [dir] as `Family-Style-subset.woff2` files, the names the
 * `@font-face` rules of `xemantic.css` refer to, together with the
 * `Family-LICENSE.txt` license, [license] being the directory of the
 * family in the Google Fonts repository: `OFL` by default, `UFL` for
 * the Ubuntu fonts.
 */
@CacheableTask
abstract class GenerateGoogleFont : DownloadTask() {

    @get:Input
    abstract val family: Property<String>

    @get:Input
    abstract val styles: ListProperty<String>

    @get:Input
    abstract val subsets: ListProperty<String>

    @get:Input
    abstract val license: Property<String>

    @get:Internal
    abstract val dir: DirectoryProperty

    init {
        license.convention("OFL")
    }

    private val fileFamily get() = family.get().replace(" ", "")

    private val licenseFile get() = dir.get().file("$fileFamily-${license.get()}.txt")

    private fun fontFile(style: String, subset: String): RegularFile {
        val (italic, weight) = style.split(",")
        val weightName = when (weight) {
            "100" -> "Thin"
            "200" -> "ExtraLight"
            "300" -> "Light"
            "400" -> "Regular"
            "500" -> "Medium"
            "600" -> "SemiBold"
            "700" -> "Bold"
            "800" -> "ExtraBold"
            "900" -> "Black"
            else -> error("Unknown font weight: $weight")
        }
        val styleName = when {
            italic == "0" -> weightName
            weightName == "Regular" -> "Italic"
            else -> "${weightName}Italic"
        }
        return dir.get().file("$fileFamily-$styleName-$subset.woff2")
    }

    @get:OutputFiles
    val files: List<RegularFile>
        get() = styles.get().flatMap { style ->
            subsets.get().map { subset -> fontFile(style, subset) }
        } + licenseFile

    @TaskAction
    fun generate() {
        val familyParam = family.get().replace(" ", "+")
        val fontCss = get(
            "https://fonts.googleapis.com/css2?family=$familyParam:ital,wght@${styles.get().sorted().joinToString(";")}"
        ).decodeToString()
        // every @font-face rule is preceded by a comment naming its subset
        val fontFaces = Regex("""/\* ([a-z-]+) \*/\s*@font-face \{([^}]*)}""")
            .findAll(fontCss)
            .associate { match ->
                val (subset, rule) = match.destructured
                fun property(name: String) = Regex("""$name: ([^;]+);""").find(rule)!!.groupValues[1]
                val italic = if (property("font-style") == "italic") "1" else "0"
                "$italic,${property("font-weight")}" to subset to
                    Regex("""url\((https://[^)]+)\) format\('woff2'\)""").find(rule)!!.groupValues[1]
            }
        styles.get().forEach { style ->
            subsets.get().forEach { subset ->
                val url = fontFaces[style to subset]
                    ?: error("No $style $subset woff2 font in the Google Fonts response:\n$fontCss")
                fontFile(style, subset).asFile.apply { parentFile.mkdirs() }.writeBytes(get(url))
            }
        }
        licenseFile.asFile.writeBytes(
            get(
                "https://raw.githubusercontent.com/google/fonts/main/" +
                    "${license.get().lowercase()}/${fileFamily.lowercase()}/${license.get()}.txt"
            )
        )
    }

}

tasks.register<GenerateGoogleFont>("generateArsenal") {
    description = "Generates the Arsenal font files used by the website."
    group = "website"
    family = "Arsenal"
    // regular, bold, italic and bold italic
    styles = listOf("0,400", "0,700", "1,400", "1,700")
    // the latter covering Polish and German
    subsets = listOf("latin", "latin-ext")
    dir = generatedWebsiteDir.map { it.dir("assets/fonts") }
}

tasks.register<GenerateGoogleFont>("generateUbuntuMono") {
    description = "Generates the Ubuntu Mono font file used by the code of the website."
    group = "website"
    family = "Ubuntu Mono"
    // the Ubuntu Font Licence, not the OFL of most Google Fonts
    license = "UFL"
    // regular only, in the subset covering source code
    styles = listOf("0,400")
    subsets = listOf("latin")
    dir = generatedWebsiteDir.map { it.dir("assets/fonts") }
}

/**
 * The Prism components concatenated, after its core, into `assets/js/prism.js`:
 * the grammars of the languages of the fenced code blocks of the site,
 * named as in https://prismjs.com/#supported-languages. A grammar extending
 * another one, like `typescript` extending `javascript`, itself extending
 * `clike`, has to follow it.
 * `checkCodeLanguages`, run after every `./gradlew run`, fails the build
 * when a rendered page holds a code block in a language missing here.
 */
val prismComponents = listOf(
    "markup",
    "clike",
    "javascript",
    "bash",
    "cypher",
    "json",
    "kotlin",
    "markdown",
    "mermaid",
    "python",
    "typescript"
)

/**
 * Downloads the core of Prism [version] and its [components], and
 * concatenates them into [js], a single script highlighting, once loaded,
 * every code block of the page.
 */
@CacheableTask
abstract class GeneratePrism : DownloadTask() {

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val components: ListProperty<String>

    @get:OutputFile
    abstract val js: RegularFileProperty

    @TaskAction
    fun generate() {
        val cdn = "https://cdn.jsdelivr.net/npm/prismjs@${version.get()}/components"
        js.get().asFile.writeText(buildString {
            append("/*\n")
            append(" * Generated by `./gradlew generatePrism`, do not edit.\n")
            append(" * Prism ${version.get()} (MIT License, https://github.com/PrismJS/prism),\n")
            append(" * components: ${components.get().joinToString()}.\n")
            append(" */\n")
            (listOf("core") + components.get()).forEach { component ->
                append(get("$cdn/prism-$component.min.js").decodeToString().trim())
                append("\n")
            }
        })
    }

}

tasks.register<GeneratePrism>("generatePrism") {
    description = "Generates Prism with the grammars of the languages used by the code blocks of the website."
    group = "website"
    version = libs.versions.prism
    components = prismComponents
    js = generatedWebsiteDir.map { it.file("assets/js/prism.js") }
}

/**
 * Fails when a page rendered with BeerCSS holds a code block in a language
 * which is not among the [components], so that Prism would leave it plain.
 */
abstract class CheckCodeLanguages : DefaultTask() {

    @get:Input
    abstract val components: ListProperty<String>

    @get:InputDirectory
    abstract val website: DirectoryProperty

    @TaskAction
    fun check() {
        val languagePattern = Regex("""<code class="language-([^"\s]+)""")
        val used = website.get().asFile.walkTopDown()
            .filter { it.extension == "html" }
            .map { it.readText() }
            .filter { "/assets/css/beercss.css" in it }
            .flatMap { html -> languagePattern.findAll(html).map { it.groupValues[1] } }
            .toSet()
        val missing = used - components.get().toSet()
        check(missing.isEmpty()) {
            "Languages of code blocks without a Prism grammar: ${missing.sorted().joinToString()}. " +
                "Add them to prismComponents in build.gradle.kts"
        }
    }

}

val checkCodeLanguages = tasks.register<CheckCodeLanguages>("checkCodeLanguages") {
    description = "Checks that every language of a code block on the rendered pages has its Prism grammar."
    group = "verification"
    components = prismComponents
    website = websiteDir
}

/**
 * Fails when a page rendered with BeerCSS uses a Material Symbols icon
 * which is not in [icons], so the vendored font lacks its glyph.
 */
abstract class CheckMaterialSymbols : DefaultTask() {

    @get:Input
    abstract val icons: ListProperty<String>

    @get:InputDirectory
    abstract val website: DirectoryProperty

    @TaskAction
    fun check() {
        val iconPattern = Regex("""<i(?:\s[^>]*)?>\s*([a-z0-9_]+)\s*</i>""")
        val used = website.get().asFile.walkTopDown()
            .filter { it.extension == "html" }
            .map { it.readText() }
            .filter { "/assets/css/beercss.css" in it }
            .flatMap { html -> iconPattern.findAll(html).map { it.groupValues[1] } }
            .toSet()
        val missing = used - icons.get().toSet()
        check(missing.isEmpty()) {
            "Material Symbols used but not vendored: ${missing.sorted().joinToString()}. " +
                "Add them to materialSymbols in build.gradle.kts"
        }
        (icons.get().toSet() - used).takeIf { it.isNotEmpty() }?.let { unused ->
            logger.warn("Material Symbols vendored but not used: ${unused.sorted().joinToString()}")
        }
    }

}

val checkMaterialSymbols = tasks.register<CheckMaterialSymbols>("checkMaterialSymbols") {
    description = "Checks that every Material Symbols icon on the rendered pages is vendored."
    group = "verification"
    icons = materialSymbols
    website = websiteDir
}

/**
 * Fails when a published Markdown document links to a page or a file of
 * the [siteUrl] missing from the [website]. The build publishes every
 * link absolute, so the check needs no knowledge of where a document
 * sits, and a URL resolves as GitHub Pages serves it: the file itself,
 * the `index.html` of a directory, or the `.html` of an extensionless
 * path. Code blocks and code spans are skipped, as their URLs are text.
 */
abstract class CheckMarkdownLinks : DefaultTask() {

    @get:Input
    abstract val siteUrl: Property<String>

    @get:InputDirectory
    abstract val website: DirectoryProperty

    @TaskAction
    fun check() {
        val root = website.get().asFile
        val prefix = siteUrl.get().removeSuffix("/") + "/"
        val codePattern = Regex("""(?s)^ {0,3}(`{3,}|~{3,}).*?^ {0,3}\1|(`+).*?\2""", RegexOption.MULTILINE)
        val linkPattern = Regex("""]\(\s*<?([^)\s>]+)|\b(?:href|src|poster)="([^"]*)"""")
        val broken = root.walkTopDown()
            .filter { it.extension == "md" }
            .flatMap { file ->
                val text = file.readText().replace(codePattern, "")
                linkPattern.findAll(text)
                    .map { it.groupValues[1].ifEmpty { it.groupValues[2] } }
                    .filter { it.startsWith(prefix) }
                    .filterNot { url ->
                        val path = URI(url).path.removePrefix("/")
                        val target = File(root, path)
                        target.isFile || File(target, "index.html").isFile || File(root, "$path.html").isFile
                    }
                    .map { "${file.relativeTo(root)}: $it" }
            }
            .distinct()
            .toList()
        check(broken.isEmpty()) {
            "Published Markdown links to missing site files:\n${broken.joinToString("\n")}"
        }
    }

}

// the script-level siteUrl, shadowed by the task property within the configuration block
val publishedSiteUrl = siteUrl

val checkMarkdownLinks = tasks.register<CheckMarkdownLinks>("checkMarkdownLinks") {
    description = "Checks that every site link in the published Markdown resolves to a published file."
    group = "verification"
    siteUrl = publishedSiteUrl
    website = websiteDir
}

tasks.run.configure {
    finalizedBy(checkMaterialSymbols, checkCodeLanguages, checkMarkdownLinks)
}
