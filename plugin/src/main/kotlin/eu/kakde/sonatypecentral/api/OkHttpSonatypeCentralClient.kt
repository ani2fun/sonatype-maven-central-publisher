package eu.kakde.sonatypecentral.api

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

class OkHttpSonatypeCentralClient(
    private val credentials: Credentials,
    baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: OkHttpClient = defaultClient(),
) : SonatypeCentralClient {
    private val baseHttpUrl: HttpUrl = baseUrl.toHttpUrl()
    private val gson = Gson()

    override fun upload(
        zipFile: File,
        coordinates: ArtifactCoordinates,
        publishingType: PublishingType,
    ): DeploymentId {
        val url =
            baseHttpUrl.newBuilder()
                .addPathSegment(PATH_UPLOAD)
                .addQueryParameter("publishingType", publishingType.name)
                .addQueryParameter("name", coordinates.toBundleName())
                .build()

        val body =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "bundle",
                    zipFile.name,
                    zipFile.asRequestBody("application/zip".toMediaType()),
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", authHeader())
                .build()

        val responseBody = execute(request)
        return parseDeploymentId(responseBody)
    }

    override fun status(deploymentId: String): DeploymentStatus {
        val url =
            baseHttpUrl.newBuilder()
                .addPathSegment(PATH_STATUS)
                .addQueryParameter("id", deploymentId)
                .build()

        val request =
            Request.Builder()
                .url(url)
                .post("".toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", authHeader())
                .build()

        val responseBody = execute(request)
        return gson.fromJson(responseBody, DeploymentStatus::class.java)
            ?: throw SonatypeApiException(0, "Empty status response", responseBody)
    }

    override fun drop(deploymentId: String) {
        val url =
            baseHttpUrl.newBuilder()
                .addPathSegment(PATH_DEPLOYMENT)
                .addPathSegment(deploymentId)
                .build()

        val request =
            Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", authHeader())
                .build()

        execute(request)
    }

    private fun execute(request: Request): String {
        val response = httpClient.newCall(request).execute()
        return response.use { resp -> readOrThrow(resp) }
    }

    private fun readOrThrow(response: Response): String {
        val body = response.body?.string().orEmpty()
        if (response.isSuccessful) {
            return body
        }
        val message =
            try {
                gson.fromJson(body, ErrorEnvelope::class.java)?.error?.message
            } catch (_: JsonSyntaxException) {
                null
            } ?: "Unknown Error: $body"
        throw SonatypeApiException(response.code, message, body)
    }

    private fun authHeader(): String {
        val raw = "${credentials.username}:${credentials.password}"
        val encoded = Base64.getEncoder().encodeToString(raw.toByteArray())
        return "UserToken $encoded"
    }

    private fun parseDeploymentId(body: String): DeploymentId {
        val raw =
            try {
                gson.fromJson(body, String::class.java)
            } catch (_: JsonSyntaxException) {
                null
            }
        return DeploymentId(raw ?: body.trim())
    }

    private data class ErrorEnvelope(val error: ErrorBody?)

    private data class ErrorBody(val message: String?)

    companion object {
        const val DEFAULT_BASE_URL = "https://central.sonatype.com/api/v1/publisher/"

        private const val PATH_UPLOAD = "upload"
        private const val PATH_STATUS = "status"
        private const val PATH_DEPLOYMENT = "deployment"

        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    },
                )
                .build()
    }
}
