package eu.kakde.sonatypecentral.utils

import org.gradle.api.GradleException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    /**
     * Archive [folderPath] into [zipFilePath].
     *
     * Throws [GradleException] if the source folder is missing or is not a
     * directory — previously this was silently swallowed (returned with a
     * println), which caused a downstream `publishToSonatype` to upload a
     * stale or missing zip without any signal that an earlier step failed.
     *
     * Overwrites [zipFilePath] if it already exists, so re-running
     * `createZip` after a partial failure produces a fresh archive. This is
     * the Gradle convention for ZIP-producing tasks.
     */
    fun prepareZipFile(
        folderPath: String,
        zipFilePath: String,
    ) {
        val sourceFolder = File(folderPath)
        val zipFile = File(zipFilePath)

        if (!sourceFolder.exists() || !sourceFolder.isDirectory) {
            throw GradleException(
                "Cannot create zip: source folder does not exist or is not a directory: ${sourceFolder.absolutePath}",
            )
        }

        if (zipFile.exists() && !zipFile.delete()) {
            throw GradleException("Cannot replace existing zip file: ${zipFile.absolutePath}")
        }

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            zipDirectory(sourceFolder, sourceFolder, zipOut)
        }

        println("Zip file created successfully at location: ${zipFile.absolutePath}")
    }

    private fun zipDirectory(
        baseDir: File,
        sourceFolder: File,
        zipOut: ZipOutputStream,
    ) {
        val data = ByteArray(1024)
        val files = sourceFolder.listFiles()

        files?.forEach { file ->
            val relativePath = baseDir.toPath().relativize(file.toPath()).toString()
            val entryName = if (relativePath.isEmpty()) file.name else relativePath.replace(File.separatorChar, '/')

            if (file.isDirectory) {
                zipDirectory(baseDir, file, zipOut)
            } else {
                FileInputStream(file).use { fi ->
                    zipOut.putNextEntry(ZipEntry(entryName))
                    var length = fi.read(data)
                    while (length != -1) {
                        zipOut.write(data, 0, length)
                        length = fi.read(data)
                    }
                }
            }
        }
    }
}
