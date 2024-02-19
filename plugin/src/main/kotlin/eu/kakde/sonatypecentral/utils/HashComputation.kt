package eu.kakde.sonatypecentral.utils

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

object HashComputation {
    fun computeAndSaveDirectoryHashes(directory: File) {
        directory.listFiles { _, name -> !name.endsWith(".asc") }?.forEach { file ->
            val md5Sum =
                HashUtils.getCheckSumFromFile(
                    MessageDigest.getInstance(MessageDigestAlgorithm.MD5),
                    file,
                )
            val sha1Sum =
                HashUtils.getCheckSumFromFile(
                    MessageDigest.getInstance(MessageDigestAlgorithm.SHA_1),
                    file,
                )

            val md5File = File(directory, "${file.name}.md5")
            val sha1File = File(directory, "${file.name}.sha1")

            writeContentToFile(md5File, md5Sum)
            writeContentToFile(sha1File, sha1Sum)
        }
    }

    private fun writeContentToFile(
        file: File,
        content: String,
    ) {
        file.bufferedWriter(UTF_8).use { writer ->
            writer.write(content)
        }
    }
}
