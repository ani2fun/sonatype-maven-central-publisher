package eu.kakde.sonatypecentral.utils

object ENDPOINT {
    private const val PUBLISHER_BASEURL = "https://central.sonatype.com/api/v1/publisher"

    const val UPLOAD = "$PUBLISHER_BASEURL/upload"

    const val STATUS = "$PUBLISHER_BASEURL/status"

    const val PUBLISHED = "$PUBLISHER_BASEURL/published" // to publish the deployment which is been validated

    const val DEPLOYMENT = "$PUBLISHER_BASEURL/deployment" // e.g. {api/v1/publisher/deployment/{deploymentId}}
}
