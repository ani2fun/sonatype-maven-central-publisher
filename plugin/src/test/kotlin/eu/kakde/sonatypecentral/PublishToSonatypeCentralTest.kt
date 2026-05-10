package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.api.ArtifactCoordinates
import eu.kakde.sonatypecentral.api.DeploymentId
import eu.kakde.sonatypecentral.api.FakeSonatypeCentralClient
import eu.kakde.sonatypecentral.api.PublishingType
import eu.kakde.sonatypecentral.api.SonatypeApiException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PublishToSonatypeCentralTest {
    @Test
    fun `passes coordinates and publishing type to the client`() {
        val project = ProjectBuilder.builder().build()
        val ext =
            project.extensions.create(
                "sonatypeCentralPublishExtension",
                SonatypeCentralPublishExtension::class.java,
            )
        ext.groupId.set("com.example")
        ext.artifactId.set("lib")
        ext.version.set("1.0.0")
        ext.publishingType.set("AUTOMATIC")

        val fake = FakeSonatypeCentralClient().apply { nextDeploymentId = DeploymentId("dep-1") }
        val task =
            project.tasks
                .register("publishToSonatype", PublishToSonatypeCentral::class.java)
                .get()
        task.client = fake

        task.uploadZip()

        assertEquals(1, fake.uploadCalls.size)
        val call = fake.uploadCalls.first()
        assertEquals(ArtifactCoordinates("com.example", "lib", "1.0.0"), call.coordinates)
        assertEquals(PublishingType.AUTOMATIC, call.publishingType)
    }

    @Test
    fun `propagates SonatypeApiException so Gradle marks the task FAILED`() {
        val project = ProjectBuilder.builder().build()
        val ext =
            project.extensions.create(
                "sonatypeCentralPublishExtension",
                SonatypeCentralPublishExtension::class.java,
            )
        ext.groupId.set("g")
        ext.artifactId.set("a")
        ext.version.set("1.0")
        ext.publishingType.set("USER_MANAGED")

        val fake = FakeSonatypeCentralClient()
        fake.failNextWith = SonatypeApiException(400, "Wrong token", """{"error":{"message":"Wrong token"}}""")

        val task =
            project.tasks
                .register("publishToSonatype", PublishToSonatypeCentral::class.java)
                .get()
        task.client = fake

        val ex = assertThrows<SonatypeApiException> { task.uploadZip() }
        assertEquals(400, ex.statusCode)
        assertEquals("Wrong token", ex.message)
    }

    @Test
    fun `accepts lowercase publishingType from the extension`() {
        val project = ProjectBuilder.builder().build()
        val ext =
            project.extensions.create(
                "sonatypeCentralPublishExtension",
                SonatypeCentralPublishExtension::class.java,
            )
        ext.groupId.set("g")
        ext.artifactId.set("a")
        ext.version.set("1.0")
        ext.publishingType.set("automatic")

        val fake = FakeSonatypeCentralClient()
        val task =
            project.tasks
                .register("publishToSonatype", PublishToSonatypeCentral::class.java)
                .get()
        task.client = fake

        task.uploadZip()

        assertEquals(PublishingType.AUTOMATIC, fake.uploadCalls.first().publishingType)
    }
}
