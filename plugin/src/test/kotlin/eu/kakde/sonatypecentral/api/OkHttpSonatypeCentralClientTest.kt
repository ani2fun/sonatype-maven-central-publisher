package eu.kakde.sonatypecentral.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.Base64

class OkHttpSonatypeCentralClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpSonatypeCentralClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            OkHttpSonatypeCentralClient(
                credentials = Credentials("alice", "secret"),
                baseUrl = server.url("/").toString(),
            )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `upload returns deployment id and sends correct request`(
        @TempDir tempDir: Path,
    ) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("\"abc-123\""))
        val zip = tempDir.resolve("upload.zip").toFile().apply { writeText("zip-bytes") }

        val result =
            client.upload(
                zipFile = zip,
                coordinates = ArtifactCoordinates("com.example", "lib", "1.0.0"),
                publishingType = PublishingType.AUTOMATIC,
            )

        assertEquals(DeploymentId("abc-123"), result)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/upload?publishingType=AUTOMATIC&name=com.example%3Alib%3A1.0.0", recorded.path)
        assertEquals(expectedAuthHeader("alice", "secret"), recorded.getHeader("Authorization"))

        val bodyText = recorded.body.readUtf8()
        assertTrue(bodyText.contains("name=\"bundle\""), "multipart body should contain bundle part: $bodyText")
        assertTrue(bodyText.contains("zip-bytes"), "multipart body should contain the zip content")
    }

    @Test
    fun `upload accepts plain-text deployment id (no JSON quotes)`(
        @TempDir tempDir: Path,
    ) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("plain-id-456"))
        val zip = tempDir.resolve("upload.zip").toFile().apply { writeText("data") }

        val result =
            client.upload(
                zipFile = zip,
                coordinates = ArtifactCoordinates("g", "a", "v"),
                publishingType = PublishingType.USER_MANAGED,
            )

        assertEquals(DeploymentId("plain-id-456"), result)
    }

    @Test
    fun `upload throws SonatypeApiException on non-2xx response`(
        @TempDir tempDir: Path,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":{"message":"Wrong token"}}"""),
        )
        val zip = tempDir.resolve("upload.zip").toFile().apply { writeText("data") }

        val ex =
            assertThrows<SonatypeApiException> {
                client.upload(
                    zipFile = zip,
                    coordinates = ArtifactCoordinates("g", "a", "v"),
                    publishingType = PublishingType.AUTOMATIC,
                )
            }

        assertEquals(400, ex.statusCode)
        assertEquals("Wrong token", ex.message)
        assertNotNull(ex.rawResponse)
    }

    @Test
    fun `status returns parsed DeploymentStatus`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "deploymentId": "abc-123",
                  "deploymentName": "g:a:1.0",
                  "deploymentState": "PENDING",
                  "purls": ["pkg:maven/g/a@1.0"],
                  "errors": {"common": ["x"]}
                }
                """.trimIndent(),
            ),
        )

        val status = client.status("abc-123")

        assertEquals("abc-123", status.deploymentId)
        assertEquals("g:a:1.0", status.deploymentName)
        assertEquals("PENDING", status.deploymentState)
        assertEquals(listOf("pkg:maven/g/a@1.0"), status.purls)
        assertEquals(mapOf("common" to listOf("x")), status.errors)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/status?id=abc-123", recorded.path)
        assertEquals(expectedAuthHeader("alice", "secret"), recorded.getHeader("Authorization"))
    }

    @Test
    fun `status throws on non-2xx response`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":{"message":"Not found"}}"""))

        val ex = assertThrows<SonatypeApiException> { client.status("missing") }
        assertEquals(404, ex.statusCode)
        assertEquals("Not found", ex.message)
    }

    @Test
    fun `drop sends DELETE to deployment path`() {
        server.enqueue(MockResponse().setResponseCode(204))

        client.drop("abc-123")

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/deployment/abc-123", recorded.path)
        assertEquals(expectedAuthHeader("alice", "secret"), recorded.getHeader("Authorization"))
    }

    @Test
    fun `drop throws on non-2xx response`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":{"message":"Forbidden"}}"""))

        val ex = assertThrows<SonatypeApiException> { client.drop("abc-123") }
        assertEquals(403, ex.statusCode)
        assertEquals("Forbidden", ex.message)
    }

    @Test
    fun `non-JSON error body falls back to raw text`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val ex = assertThrows<SonatypeApiException> { client.status("any") }
        assertEquals(500, ex.statusCode)
        assertEquals("Unknown Error: Internal Server Error", ex.message)
    }

    private fun expectedAuthHeader(
        username: String,
        password: String,
    ): String {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return "UserToken $encoded"
    }
}
