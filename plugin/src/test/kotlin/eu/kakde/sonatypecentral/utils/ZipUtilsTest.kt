package eu.kakde.sonatypecentral.utils

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

class ZipUtilsTest {
    @Test
    fun `archives the contents of a directory`(
        @TempDir tempDir: Path,
    ) {
        val source = tempDir.resolve("staging").toFile().apply { mkdirs() }
        File(source, "a.txt").writeText("alpha")
        File(source, "b.txt").writeText("beta")
        val zipFile = tempDir.resolve("out.zip").toFile()

        ZipUtils.prepareZipFile(source.path, zipFile.path)

        assertTrue(zipFile.exists())
        ZipFile(zipFile).use { zf ->
            val names = zf.entries().toList().map { it.name }.sorted()
            assertEquals(listOf("a.txt", "b.txt"), names)
        }
    }

    @Test
    fun `throws GradleException when source folder does not exist`(
        @TempDir tempDir: Path,
    ) {
        val missing = tempDir.resolve("does-not-exist").toFile()
        val zipFile = tempDir.resolve("out.zip").toFile()

        val ex =
            assertThrows<GradleException> {
                ZipUtils.prepareZipFile(missing.path, zipFile.path)
            }
        assertTrue(ex.message!!.contains("does not exist"))
    }

    @Test
    fun `throws GradleException when source path is a file rather than a directory`(
        @TempDir tempDir: Path,
    ) {
        val notADir = tempDir.resolve("a.txt").toFile().apply { writeText("oops") }
        val zipFile = tempDir.resolve("out.zip").toFile()

        assertThrows<GradleException> {
            ZipUtils.prepareZipFile(notADir.path, zipFile.path)
        }
    }

    @Test
    fun `overwrites an existing zip file so reruns are idempotent`(
        @TempDir tempDir: Path,
    ) {
        // Regression for the silent-failure bug: previously the function
        // returned silently if the zip existed, leaving a stale archive in
        // place. Now we overwrite.
        val source = tempDir.resolve("staging").toFile().apply { mkdirs() }
        File(source, "a.txt").writeText("first run")
        val zipFile = tempDir.resolve("out.zip").toFile()

        ZipUtils.prepareZipFile(source.path, zipFile.path)
        val firstSize = zipFile.length()

        // Second run with different contents — the zip should be replaced,
        // not silently kept.
        File(source, "a.txt").writeText("second run, much longer text, different size on purpose")
        ZipUtils.prepareZipFile(source.path, zipFile.path)

        assertTrue(zipFile.exists())
        ZipFile(zipFile).use { zf ->
            val entry = zf.getEntry("a.txt")
            val content = zf.getInputStream(entry).bufferedReader().readText()
            assertEquals("second run, much longer text, different size on purpose", content)
        }
        // Sanity check: the file did change, not just match-by-name.
        assertTrue(zipFile.length() != firstSize)
    }
}
