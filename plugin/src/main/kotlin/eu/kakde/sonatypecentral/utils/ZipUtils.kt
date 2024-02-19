package eu.kakde.sonatypecentral.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun prepareZipFile(
        folderPath: String,
        zipFilePath: String,
    ) {
        val sourceFolder = File(folderPath)
        val zipFile = File(zipFilePath)

        if (!sourceFolder.exists()) {
            println("Source folder does not exist.")
            return
        }

        if (zipFile.exists()) {
            println("Zip file already exists. Please provide a different path.")
            return
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
