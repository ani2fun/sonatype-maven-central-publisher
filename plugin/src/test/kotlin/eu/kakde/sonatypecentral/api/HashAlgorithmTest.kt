package eu.kakde.sonatypecentral.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.MessageDigest

class HashAlgorithmTest {
    @Test
    fun `every javaName resolves to a real JDK MessageDigest`() {
        HashAlgorithm.entries.forEach { algorithm ->
            // Throws NoSuchAlgorithmException if the JDK doesn't recognise the
            // name — guards against typos in the enum table.
            MessageDigest.getInstance(algorithm.javaName)
        }
    }

    @Test
    fun `file suffixes contain no characters that would break a filename`() {
        HashAlgorithm.entries.forEach { algorithm ->
            val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
            invalidChars.forEach { ch ->
                assertTrue(
                    !algorithm.fileSuffix.contains(ch),
                    "${algorithm.name} fileSuffix '${algorithm.fileSuffix}' contains '$ch'",
                )
            }
        }
    }

    @Test
    fun `fromJavaName resolves common algorithms`() {
        assertEquals(HashAlgorithm.MD5, HashAlgorithm.fromJavaName("MD5"))
        assertEquals(HashAlgorithm.SHA_1, HashAlgorithm.fromJavaName("SHA-1"))
        assertEquals(HashAlgorithm.SHA_256, HashAlgorithm.fromJavaName("SHA-256"))
        assertEquals(HashAlgorithm.SHA_512, HashAlgorithm.fromJavaName("SHA-512"))
    }

    @Test
    fun `fromJavaName throws with the full supported list on an unknown algorithm`() {
        val ex =
            assertThrows<IllegalArgumentException> {
                HashAlgorithm.fromJavaName("MD-256")
            }
        // Message should name the bad input AND list valid names so the user
        // can self-serve without digging into source.
        assertTrue(ex.message!!.contains("'MD-256'"))
        assertTrue(ex.message!!.contains("SHA-256"))
    }
}
