import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
        versions = setOf("kotlinTarget", "javaTarget")
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

tasks.run.configure {
    description = "Renders the whole website into the build folder."
    args(
        layout.projectDirectory.asFile.absolutePath,
        layout.buildDirectory.dir("website").get().asFile.absolutePath
    )
}

tasks.register<JavaExec>("serve") {
    description = "Serves the website live from the source, rendering Markdown on each request."
    group = ApplicationPlugin.APPLICATION_GROUP
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.xemantic.website.ServerKt"
    args(layout.projectDirectory.asFile.absolutePath)
}
