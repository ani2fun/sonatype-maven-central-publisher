package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.api.DeploymentStatus
import eu.kakde.sonatypecentral.api.FakeSonatypeCentralClient
import eu.kakde.sonatypecentral.api.SonatypeApiException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetDeploymentStatusTest {
    @Test
    fun `passes deploymentId to the client`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.create("sonatypeCentralPublishExtension", SonatypeCentralPublishExtension::class.java)

        val fake =
            FakeSonatypeCentralClient().apply {
                nextStatus =
                    DeploymentStatus(
                        deploymentId = "abc-123",
                        deploymentName = "g:a:1.0",
                        deploymentState = "PENDING",
                        purls = emptyList(),
                        errors = null,
                    )
            }

        val task =
            project.tasks
                .register("getDeploymentStatus", GetDeploymentStatus::class.java)
                .get()
        task.client = fake
        task.deploymentId = "abc-123"

        task.executeTask()

        assertEquals(listOf("abc-123"), fake.statusCalls)
    }

    @Test
    fun `propagates SonatypeApiException`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.create("sonatypeCentralPublishExtension", SonatypeCentralPublishExtension::class.java)

        val fake = FakeSonatypeCentralClient()
        fake.failNextWith = SonatypeApiException(404, "Not found", null)

        val task =
            project.tasks
                .register("getDeploymentStatus", GetDeploymentStatus::class.java)
                .get()
        task.client = fake
        task.deploymentId = "missing"

        val ex = assertThrows<SonatypeApiException> { task.executeTask() }
        assertEquals(404, ex.statusCode)
    }
}
