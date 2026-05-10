package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.api.FakeSonatypeCentralClient
import eu.kakde.sonatypecentral.api.SonatypeApiException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DropDeploymentTest {
    @Test
    fun `passes deploymentId to the client`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.create("sonatypeCentralPublishExtension", SonatypeCentralPublishExtension::class.java)

        val fake = FakeSonatypeCentralClient()

        val task =
            project.tasks
                .register("dropDeployment", DropDeployment::class.java)
                .get()
        task.client = fake
        task.deploymentId = "abc-123"

        task.executeTask()

        assertEquals(listOf("abc-123"), fake.dropCalls)
    }

    @Test
    fun `propagates SonatypeApiException`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.create("sonatypeCentralPublishExtension", SonatypeCentralPublishExtension::class.java)

        val fake = FakeSonatypeCentralClient()
        fake.failNextWith = SonatypeApiException(403, "Forbidden", null)

        val task =
            project.tasks
                .register("dropDeployment", DropDeployment::class.java)
                .get()
        task.client = fake
        task.deploymentId = "abc-123"

        val ex = assertThrows<SonatypeApiException> { task.executeTask() }
        assertEquals(403, ex.statusCode)
    }
}
