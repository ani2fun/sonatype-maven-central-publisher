package eu.kakde.sonatypecentral.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class BundleLayoutTest {
    @Test
    fun `from() builds upload root, staging dir, and zip path consistently`() {
        val build = File("/tmp/project/build")
        val coords = ArtifactCoordinates("com.example", "lib", "1.0.0")

        val layout = BundleLayout.from(build, coords)

        assertEquals(File("/tmp/project/build/upload"), layout.uploadRootDirectory)
        assertEquals(
            File("/tmp/project/build/upload/com/example/lib/1.0.0"),
            layout.stagingDirectory,
        )
        assertEquals(File("/tmp/project/build/upload.zip"), layout.zipFile)
        assertEquals(coords, layout.coordinates)
    }

    @Test
    fun `staging dir nests deeper for multi-segment groupIds`() {
        val build = File("/tmp/build")
        val coords = ArtifactCoordinates("eu.kakde.plugindemo", "samplelib", "1.0.3")

        val layout = BundleLayout.from(build, coords)

        // Three group segments produce three dirs under the upload root, before
        // the artifactId/version pair. Regression for the parentFile.parentFile
        // approach that only worked for two-segment groupIds.
        assertEquals(
            File("/tmp/build/upload/eu/kakde/plugindemo/samplelib/1.0.3"),
            layout.stagingDirectory,
        )
        assertEquals(
            File("/tmp/build/upload"),
            layout.uploadRootDirectory,
        )
    }
}
