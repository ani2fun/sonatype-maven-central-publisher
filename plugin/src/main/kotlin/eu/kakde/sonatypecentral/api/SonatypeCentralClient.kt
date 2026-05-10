package eu.kakde.sonatypecentral.api

import java.io.File

interface SonatypeCentralClient {
    fun upload(
        zipFile: File,
        coordinates: ArtifactCoordinates,
        publishingType: PublishingType,
    ): DeploymentId

    fun status(deploymentId: String): DeploymentStatus

    fun drop(deploymentId: String)
}

data class ArtifactCoordinates(
    val groupId: String,
    val artifactId: String,
    val version: String,
) {
    fun toBundleName(): String = "$groupId:$artifactId:$version"
}

enum class PublishingType { AUTOMATIC, USER_MANAGED }

@JvmInline
value class DeploymentId(val value: String)

data class DeploymentStatus(
    val deploymentId: String,
    val deploymentName: String?,
    val deploymentState: String,
    val purls: List<String>,
    val errors: Map<String, List<String>>?,
)

data class Credentials(
    val username: String,
    val password: String,
) {
    init {
        require(username.isNotBlank()) { "Sonatype username must not be empty" }
        require(password.isNotBlank()) { "Sonatype password must not be empty" }
    }
}
