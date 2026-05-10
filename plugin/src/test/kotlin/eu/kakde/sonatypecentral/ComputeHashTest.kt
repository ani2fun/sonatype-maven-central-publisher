package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.api.ArtifactCoordinates
import eu.kakde.sonatypecentral.api.BundleLayout
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ComputeHashTest {
    @Test
    fun `writes md5 sha1 and user-requested hashes alongside each non-asc file`(
        @TempDir tempDir: Path,
    ) {
        val staging = tempDir.toFile()
        File(staging, "lib.jar").writeText("hello world")
        // Pre-existing .asc files must be skipped (they're signatures, not artifacts).
        File(staging, "lib.jar.asc").writeText("signature")

        val layout =
            BundleLayout(
                uploadRootDirectory = staging,
                stagingDirectory = staging,
                zipFile = File(tempDir.toFile(), "upload.zip"),
                coordinates = ArtifactCoordinates("g", "lib", "1.0"),
            )
        val project = ProjectBuilder.builder().build()
        val task =
            project.tasks
                .register("computeHash", ComputeHash::class.java, layout, listOf("SHA-256"))
                .get()

        task.run()

        // MD5 of "hello world" — the same value the existing HashUtilsTest pins.
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", File(staging, "lib.jar.md5").readText())
        // SHA-1 of "hello world".
        assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", File(staging, "lib.jar.sha1").readText())
        // SHA-256 of "hello world".
        assertEquals(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            File(staging, "lib.jar.sha256").readText(),
        )
        // .asc files are not hashed — no lib.jar.asc.md5 etc.
        assertFalse(File(staging, "lib.jar.asc.md5").exists())
    }

    @Test
    fun `does not double-write sha1 when user lists it explicitly`(
        @TempDir tempDir: Path,
    ) {
        val staging = tempDir.toFile()
        File(staging, "lib.jar").writeText("hello world")

        val layout =
            BundleLayout(
                uploadRootDirectory = staging,
                stagingDirectory = staging,
                zipFile = File(tempDir.toFile(), "upload.zip"),
                coordinates = ArtifactCoordinates("g", "lib", "1.0"),
            )
        val project = ProjectBuilder.builder().build()
        val task =
            project.tasks
                .register("computeHash", ComputeHash::class.java, layout, listOf("SHA-1", "SHA-256"))
                .get()

        task.run()

        // Single sha1 file — the implicit Maven Central requirement and the user's
        // explicit request are deduped.
        assertTrue(File(staging, "lib.jar.sha1").exists())
        assertTrue(File(staging, "lib.jar.sha256").exists())
        assertTrue(File(staging, "lib.jar.md5").exists())
    }
}
