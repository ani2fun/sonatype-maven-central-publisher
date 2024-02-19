package eu.kakde.sonatypecentral.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility object for computing checksums (hash values) from files or input streams.
 */
object HashUtils {
    // Constant for the buffer length when reading from input streams
    const val STREAM_BUFFER_LENGTH = 1024

    /**
     * Computes the checksum (hash value) of a file given its path.
     *
     * @param digest The MessageDigest algorithm to use (e.g. MD5)
     * @param filePath The path to the file
     * @return The checksum as a hexadecimal string
     */
    fun getCheckSumFromFile(
        digest: MessageDigest,
        filePath: String,
    ): String {
        val file = File(filePath)
        return getCheckSumFromFile(digest, file)
    }

    /**
     * Computes the checksum (hash value) of a file.
     *
     * @param digest The MessageDigest algorithm to use (e.g. MD5)
     * @param file The file for which to compute the checksum
     * @return The checksum as a hexadecimal string
     */
    fun getCheckSumFromFile(
        digest: MessageDigest,
        file: File,
    ): String {
        val fis = FileInputStream(file)
        val byteArray = updateDigest(digest, fis).digest()
        fis.close()
        val hexCode = encodeHex(byteArray, true)
        return String(hexCode)
    }

    /**
     * Updates a digest with data read from an input stream.
     *
     * @param digest The MessageDigest to update
     * @param data The input stream to read data from
     * @return The updated MessageDigest
     */
    private fun updateDigest(
        digest: MessageDigest,
        data: InputStream,
    ): MessageDigest {
        val buffer = ByteArray(STREAM_BUFFER_LENGTH)
        var read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        while (read > -1) {
            digest.update(buffer, 0, read)
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        }
        return digest
    }

    /**
     * Hexadecimal digits for lowercase representation
     */
    private val DIGITS_LOWER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    /**
     * Hexadecimal digits for uppercase representation
     */
    private val DIGITS_UPPER =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toLowerCase `true` converts to lowercase, `false` to uppercase
     * @return A char[] containing hexadecimal characters in the selected case
     */
    fun encodeHex(
        data: ByteArray,
        toLowerCase: Boolean,
    ): CharArray {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toDigits the output alphabet (must contain at least 16 chars)
     * @return A char[] containing the appropriate characters from the alphabet
     *         For best results, this should be either upper- or lower-case hex.
     */
    fun encodeHex(
        data: ByteArray,
        toDigits: CharArray,
    ): CharArray {
        val l = data.size
        val out = CharArray(l shl 1)
        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[0xF0 and data[i].toInt() ushr 4]
            out[j++] = toDigits[0x0F and data[i].toInt()]
            i++
        }
        return out
    }
}
