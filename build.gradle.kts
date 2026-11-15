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
        versions = setOf("kotlinTarget", "javaTarget", "beerCss")
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
    dependsOn("generateBeerCss", "generateArsenal")
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
    dependsOn("generateBeerCss", "generateArsenal")
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
    "helpers/form",
    "helpers/responsive",
    "helpers/wave",
    "elements/bar",
    "elements/button",
    "elements/card",
    "elements/dialog",
    "elements/divider",
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
 * `Family-OFL.txt` license.
 */
@CacheableTask
abstract class GenerateGoogleFont : DownloadTask() {

    @get:Input
    abstract val family: Property<String>

    @get:Input
    abstract val styles: ListProperty<String>

    @get:Input
    abstract val subsets: ListProperty<String>

    @get:Internal
    abstract val dir: DirectoryProperty

    private val fileFamily get() = family.get().replace(" ", "")

    private val license get() = dir.get().file("$fileFamily-OFL.txt")

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
        } + license

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
        license.asFile.writeBytes(
            get("https://raw.githubusercontent.com/google/fonts/main/ofl/${fileFamily.lowercase()}/OFL.txt")
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

tasks.run.configure {
    finalizedBy(checkMaterialSymbols)
}
