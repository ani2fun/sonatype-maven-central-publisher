package eu.kakde.sonatypecentral.api

import java.io.File

/**
 * A source file plus the name it should have inside the staged bundle.
 */
data class NamedFile(
    val source: File,
    val targetName: String,
)

/**
 * Rules for "what files make up a Sonatype Central bundle, and what are
 * their canonical names".
 *
 * Before this module these rules lived inside `AggregateFiles.action()` —
 * a ~60-line task body that required a real Gradle build to exercise, so
 * none of the filter or rename logic was unit-tested. Each function here
 * takes the files in a known Gradle output directory and returns
 * (source, targetName) pairs ready to be staged into the bundle layout.
 *
 * The functions are pure: they do not touch the file system. The caller
 * is responsible for copying (or moving) each entry into the staging
 * directory. This is a behavioural change from the previous
 * `renameFile()` approach, which moved files in place inside Gradle's own
 * output directories — copying is idempotent across reruns and leaves
 * Gradle's outputs untouched.
 */
object BundleComposition {
    /**
     * `build/libs/` — JARs produced by `jar`, `bootJar`, `sourcesJar`,
     * `javadocJar`, plus their `.asc` signatures. Spring Boot's
     * `-plain.jar` (the unrepackaged classes-only jar) is intentionally
     * excluded; it isn't meant for publication.
     */
    fun fromLibs(files: Iterable<File>): List<NamedFile> =
        files
            .filter { !it.name.endsWith("-plain.jar") && !it.name.endsWith("-plain.jar.asc") }
            .map { NamedFile(it, it.name) }

    /**
     * `build/publications/maven/` — the POM and Gradle module metadata.
     * Gradle writes these as `pom-default.xml` and `module.json`; Maven
     * Central expects `{artifactId}-{version}.pom` and `.module`.
     *
     * Any unrecognised file is prefixed with `{artifactId}-{version}.`
     * defensively, so an unexpected sibling doesn't collide with another
     * artifact in the staged directory.
     */
    fun fromPublicationsMaven(
        files: Iterable<File>,
        coords: ArtifactCoordinates,
    ): List<NamedFile> {
        val prefix = "${coords.artifactId}-${coords.version}"
        return files.map { file ->
            val newName =
                when (file.name) {
                    "pom-default.xml" -> "$prefix.pom"
                    "pom-default.xml.asc" -> "$prefix.pom.asc"
                    "module.json" -> "$prefix.module"
                    "module.json.asc" -> "$prefix.module.asc"
                    else -> "$prefix.${file.name}"
                }
            NamedFile(file, newName)
        }
    }

    /**
     * `build/version-catalog/` — the Gradle version catalog TOML. Renamed
     * to `{artifactId}-{version}.toml` and `.toml.asc`. Files that don't
     * end with `versions.toml` are passed through unchanged (matching the
     * pre-refactor behaviour).
     */
    fun fromVersionCatalog(
        files: Iterable<File>,
        coords: ArtifactCoordinates,
    ): List<NamedFile> {
        val prefix = "${coords.artifactId}-${coords.version}"
        return files.map { file ->
            val newName =
                when {
                    file.name.endsWith("versions.toml.asc") -> "$prefix.toml.asc"
                    file.name.endsWith("versions.toml") -> "$prefix.toml"
                    else -> file.name
                }
            NamedFile(file, newName)
        }
    }
}
