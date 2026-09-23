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
        freeCompilerArgs.add("-Xjdk-release=$javaTarget")
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
    implementation(libs.xemantic.kotlin.core)
}

application {
    mainClass = "com.xemantic.website.RenderKt"
}

/** The generated files published next to the website source. */
val generatedWebsiteDir = layout.buildDirectory.dir("generated/website")

tasks.run.configure {
    description = "Renders the whole website into the build folder."
    dependsOn("generateBeerCss")
    args(
        layout.projectDirectory.asFile.absolutePath,
        layout.buildDirectory.dir("website").get().asFile.absolutePath,
        generatedWebsiteDir.get().asFile.absolutePath
    )
}

tasks.register<JavaExec>("serve") {
    description = "Serves the website live from the source, rendering Markdown on each request."
    group = ApplicationPlugin.APPLICATION_GROUP
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.xemantic.website.ServerKt"
    dependsOn("generateBeerCss")
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
    "elements/bar",
    "elements/divider",
    "elements/icon",
    "elements/layout",
    "elements/mainLayout",
    "elements/navigation",
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
    "home"
)

/**
 * Downloads the [modules] of BeerCSS [version] and concatenates them into
 * [css], followed by a `@font-face` for the [icons] subset of the
 * Material Symbols Outlined font, which is downloaded into [font].
 */
@CacheableTask
abstract class GenerateBeerCss : DefaultTask() {

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
        val http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        fun get(url: String): ByteArray {
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
    website = layout.buildDirectory.dir("website")
}

tasks.run.configure {
    finalizedBy(checkMaterialSymbols)
}
