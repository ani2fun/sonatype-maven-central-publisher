package eu.kakde.sonatypecentral.api

import org.gradle.api.GradleException

class SonatypeApiException(
    val statusCode: Int,
    message: String,
    val rawResponse: String?,
) : GradleException(message)
