package eu.kakde.sonatypecentral.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class BundleCompositionTest {
    private val coords = ArtifactCoordinates("com.example", "samplelib", "1.0.0")

    private fun fakeFiles(
        dir: Path,
        names: List<String>,
    ): List<File> =
        names.map { name ->
            val file = dir.resolve(name).toFile()
            file.writeText("test")
            file
        }

    // -------- fromLibs --------

    @Test
    fun `fromLibs passes through standard jars unchanged`(
        @TempDir dir: Path,
    ) {
        val files =
            fakeFiles(
                dir,
                listOf(
                    "samplelib-1.0.0.jar",
                    "samplelib-1.0.0-sources.jar",
                    "samplelib-1.0.0-javadoc.jar",
                    "samplelib-1.0.0.jar.asc",
                ),
            )

        val result = BundleComposition.fromLibs(files)

        assertEquals(4, result.size)
        result.forEach { assertEquals(it.source.name, it.targetName) }
    }

    @Test
    fun `fromLibs excludes Spring Boot plain jars`(
        @TempDir dir: Path,
    ) {
        val files =
            fakeFiles(
                dir,
                listOf(
                    "app-1.0.0.jar",
                    "app-1.0.0-plain.jar",
                    "app-1.0.0-plain.jar.asc",
                    "app-1.0.0.jar.asc",
                ),
            )

        val result = BundleComposition.fromLibs(files).map { it.targetName }

        assertEquals(listOf("app-1.0.0.jar", "app-1.0.0.jar.asc"), result.sorted())
    }

    // -------- fromPublicationsMaven --------

    @Test
    fun `fromPublicationsMaven renames pom-default and module to Maven Central convention`(
        @TempDir dir: Path,
    ) {
        val files =
            fakeFiles(
                dir,
                listOf(
                    "pom-default.xml",
                    "pom-default.xml.asc",
                    "module.json",
                    "module.json.asc",
                ),
            )

        val result =
            BundleComposition
                .fromPublicationsMaven(files, coords)
                .associate { it.source.name to it.targetName }

        assertEquals("samplelib-1.0.0.pom", result["pom-default.xml"])
        assertEquals("samplelib-1.0.0.pom.asc", result["pom-default.xml.asc"])
        assertEquals("samplelib-1.0.0.module", result["module.json"])
        assertEquals("samplelib-1.0.0.module.asc", result["module.json.asc"])
    }

    @Test
    fun `fromPublicationsMaven defensively prefixes unknown files`(
        @TempDir dir: Path,
    ) {
        val files = fakeFiles(dir, listOf("surprise.txt"))

        val result = BundleComposition.fromPublicationsMaven(files, coords)

        assertEquals(1, result.size)
        assertEquals("samplelib-1.0.0.surprise.txt", result[0].targetName)
    }

    // -------- fromVersionCatalog --------

    @Test
    fun `fromVersionCatalog renames the versions toml and its signature`(
        @TempDir dir: Path,
    ) {
        val files = fakeFiles(dir, listOf("samplelib.versions.toml", "samplelib.versions.toml.asc"))

        val result =
            BundleComposition
                .fromVersionCatalog(files, coords)
                .associate { it.source.name to it.targetName }

        assertEquals("samplelib-1.0.0.toml", result["samplelib.versions.toml"])
        assertEquals("samplelib-1.0.0.toml.asc", result["samplelib.versions.toml.asc"])
    }

    @Test
    fun `fromVersionCatalog passes unknown files through unchanged`(
        @TempDir dir: Path,
    ) {
        val files = fakeFiles(dir, listOf("readme.md"))

        val result = BundleComposition.fromVersionCatalog(files, coords)

        assertEquals(1, result.size)
        assertEquals("readme.md", result[0].targetName)
    }

    // -------- purity --------

    @Test
    fun `composition functions do not mutate the source files`(
        @TempDir dir: Path,
    ) {
        // Regression test for the previous renameFile() approach, which moved
        // files in place inside Gradle's own output directories. The new
        // composition is pure: it returns target names, the caller copies.
        val files = fakeFiles(dir, listOf("pom-default.xml"))

        BundleComposition.fromPublicationsMaven(files, coords)

        assertTrue(File(dir.toFile(), "pom-default.xml").exists())
    }
}
