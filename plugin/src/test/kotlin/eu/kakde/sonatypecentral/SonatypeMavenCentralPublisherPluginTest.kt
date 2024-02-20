package eu.kakde.sonatypecentral

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A simple unit test for the 'eu.kakde.gradle.sonatype-maven-central-publisher' plugin.
 */
class SonatypeMavenCentralPublisherPluginTest {
    @Test fun `test plugin class apply`() {
        val project = ProjectBuilder.builder().build()
        val expected = "eu.kakde.sonatypecentral.SonatypeMavenCentralPublisherPlugin"
        val actual = project.plugins.apply("eu.kakde.gradle.sonatype-maven-central-publisher")
        assertEquals(expected, actual.javaClass.name)
    }
}
