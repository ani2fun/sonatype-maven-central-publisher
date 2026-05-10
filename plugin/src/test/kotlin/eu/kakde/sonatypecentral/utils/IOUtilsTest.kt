package eu.kakde.sonatypecentral.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class IOUtilsTest {
    @Test
    fun `createDirectoryStructure creates nested directories`(
        @TempDir tempDir: Path,
    ) {
        val target = tempDir.resolve("a/b/c").toString()

        val result = IOUtils.createDirectoryStructure(target)

        assertTrue(result.exists())
        assertTrue(result.isDirectory)
        assertEquals(target, result.path)
    }

    @Test
    fun `createDirectoryStructure does not throw on platforms that support POSIX`(
        @TempDir tempDir: Path,
    ) {
        // Regression test for #4: the original implementation called
        // Files.setPosixFilePermissions unconditionally, which threw
        // UnsupportedOperationException on Windows. The fix guards the call.
        val target = tempDir.resolve("posix-check").toString()
        IOUtils.createDirectoryStructure(target)
        // No exception means we're good on this OS; the Windows path is exercised
        // by the runtime guard, which we cannot trigger from a POSIX host.
    }

    @Test
    fun `renameFile renames existing file`(
        @TempDir tempDir: Path,
    ) {
        val original = tempDir.resolve("original.txt").toFile().apply { writeText("hi") }

        val renamed = IOUtils.renameFile(original, "renamed.txt")

        assertEquals("renamed.txt", renamed.name)
        assertTrue(renamed.exists())
        assertFalse(original.exists())
        assertEquals("hi", renamed.readText())
    }

    @Test
    fun `renameFile throws when source does not exist`(
        @TempDir tempDir: Path,
    ) {
        val missing = File(tempDir.toFile(), "missing.txt")

        assertThrows<IllegalArgumentException> {
            IOUtils.renameFile(missing, "irrelevant.txt")
        }
    }
}
