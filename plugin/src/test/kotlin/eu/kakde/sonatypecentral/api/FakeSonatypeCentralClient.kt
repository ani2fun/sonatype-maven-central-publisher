package eu.kakde.sonatypecentral.api

import java.io.File

class FakeSonatypeCentralClient : SonatypeCentralClient {
    val uploadCalls = mutableListOf<UploadCall>()
    val statusCalls = mutableListOf<String>()
    val dropCalls = mutableListOf<String>()

    var nextDeploymentId: DeploymentId = DeploymentId("fake-deployment-id")
    var nextStatus: DeploymentStatus =
        DeploymentStatus(
            deploymentId = "fake-deployment-id",
            deploymentName = "fake:fake:1.0.0",
            deploymentState = "PUBLISHED",
            purls = emptyList(),
            errors = null,
        )
    var failNextWith: SonatypeApiException? = null

    override fun upload(
        zipFile: File,
        coordinates: ArtifactCoordinates,
        publishingType: PublishingType,
    ): DeploymentId {
        uploadCalls += UploadCall(zipFile, coordinates, publishingType)
        failNextWith?.let { throw it }
        return nextDeploymentId
    }

    override fun status(deploymentId: String): DeploymentStatus {
        statusCalls += deploymentId
        failNextWith?.let { throw it }
        return nextStatus
    }

    override fun drop(deploymentId: String) {
        dropCalls += deploymentId
        failNextWith?.let { throw it }
    }
}

data class UploadCall(
    val zipFile: File,
    val coordinates: ArtifactCoordinates,
    val publishingType: PublishingType,
)
