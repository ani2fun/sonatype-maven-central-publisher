package eu.kakde.sonatypecentral.utils

import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Locale

object HashComputation {
    fun computeAndSaveDirectoryHashes(
        directory: File,
        shaAlgorithms: List<String>,
    ) {
        directory.listFiles { _, name -> !name.endsWith(".asc") }?.forEach { file ->
            // MD5 Computation and write to file
            val md5Sum =
                HashUtils.getCheckSumFromFile(
                    MessageDigest.getInstance(MessageDigestAlgorithm.MD5),
                    file,
                )
            val md5FileName = "${file.name}.${MessageDigestAlgorithm.MD5.replace("-", "").lowercase(Locale.getDefault())}"
            val md5File = File(directory, md5FileName)
            writeContentToFile(md5File, md5Sum)

            val setOfAlgorithms = (shaAlgorithms + listOf(MessageDigestAlgorithm.SHA_1)).toSet()

            // Other SHA Computation as per the values present in the shaAlgorithms list and write all to it's respective files.
            setOfAlgorithms.forEach { algorithm ->
                val shaSum =
                    HashUtils.getCheckSumFromFile(
                        MessageDigest.getInstance(algorithm),
                        file,
                    )
                val shaFileName = "${file.name}.${algorithm.replace("-", "").lowercase(Locale.getDefault())}"
                val shaFile = File(directory, shaFileName)
                writeContentToFile(shaFile, shaSum)
            }
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
