package eu.kakde.sonatypecentral.api

/**
 * Hash algorithms supported when computing checksum side-files for a bundle.
 *
 * Each entry carries both the `MessageDigest.getInstance(...)` name and the
 * file suffix used for `{artifact}.{suffix}` in the bundle, so the
 * algorithm-to-suffix mapping lives in exactly one place.
 *
 * The `fileSuffix` values follow the de-facto Maven Central convention
 * (`.md5`, `.sha1`, `.sha256`, `.sha512`). The SHA-512/N and SHA-3 variants
 * are included for completeness but rarely used in practice.
 */
enum class HashAlgorithm(val javaName: String, val fileSuffix: String) {
    MD5("MD5", "md5"),
    SHA_1("SHA-1", "sha1"),
    SHA_224("SHA-224", "sha224"),
    SHA_256("SHA-256", "sha256"),
    SHA_384("SHA-384", "sha384"),
    SHA_512("SHA-512", "sha512"),
    SHA_512_224("SHA-512/224", "sha512_224"),
    SHA_512_256("SHA-512/256", "sha512_256"),
    SHA3_224("SHA3-224", "sha3_224"),
    SHA3_256("SHA3-256", "sha3_256"),
    SHA3_384("SHA3-384", "sha3_384"),
    SHA3_512("SHA3-512", "sha3_512"),
    ;

    companion object {
        /**
         * Parse a [HashAlgorithm] from its Java name as exposed in the Gradle
         * DSL (e.g. `"SHA-256"`). Throws [IllegalArgumentException] with the
         * full list of valid names if [name] doesn't match.
         */
        fun fromJavaName(name: String): HashAlgorithm =
            entries.firstOrNull { it.javaName == name }
                ?: throw IllegalArgumentException(
                    "Unknown hash algorithm: '$name'. Supported: ${entries.joinToString { it.javaName }}",
                )
    }
}
