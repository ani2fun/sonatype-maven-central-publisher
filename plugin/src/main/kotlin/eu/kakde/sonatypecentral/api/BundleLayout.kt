package eu.kakde.sonatypecentral.api

import java.io.File

/**
 * One source of truth for where the publishable bundle lives on disk.
 *
 * Before this type, the plugin computed the staging directory path and four
 * tasks (AggregateFiles, ComputeHash, CreateZip, PublishToSonatypeCentral)
 * each kept their own copy of either the directory or the zip path. A change
 * to "where do we stage artifacts?" required edits in five places.
 */
data class BundleLayout(
    /** `build/upload/` — the root of the Maven namespace tree archived into the zip. */
    val uploadRootDirectory: File,
    /** `build/upload/{groupAsPath}/{artifactId}/{version}/` — where the artifacts and
     *  their hashes are staged before zipping. */
    val stagingDirectory: File,
    /** `build/upload.zip` — the bundle uploaded to Sonatype. */
    val zipFile: File,
    val coordinates: ArtifactCoordinates,
) {
    companion object {
        fun from(
            buildDirectory: File,
            coordinates: ArtifactCoordinates,
        ): BundleLayout {
            val uploadRoot = File(buildDirectory, "upload")
            val namespacePath = coordinates.groupId.replace('.', File.separatorChar)
            val staging =
                File(
                    uploadRoot,
                    "$namespacePath${File.separator}${coordinates.artifactId}${File.separator}${coordinates.version}",
                )
            val zip = File(buildDirectory, "upload.zip")
            return BundleLayout(uploadRoot, staging, zip, coordinates)
        }
    }
}
