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

    @Test fun `extension conventions fall back to project coordinates when unconfigured`() {
        // Regression test for #2: previously the plugin failed in afterEvaluate
        // with "Cannot query the value of extension ... property 'groupId' because
        // it has no value available" when the user applied the plugin without
        // configuring the extension. Conventions on the extension now provide
        // sensible defaults derived from the Project.
        val project = ProjectBuilder.builder().withName("samplelib").build()
        project.group = "com.example"
        project.version = "1.2.3"
        project.plugins.apply("eu.kakde.gradle.sonatype-maven-central-publisher")

        val ext = project.extensions.getByType(SonatypeCentralPublishExtension::class.java)

        assertEquals("com.example", ext.groupId.get())
        assertEquals("samplelib", ext.artifactId.get())
        assertEquals("1.2.3", ext.version.get())
        assertEquals("java", ext.componentType.get())
        assertEquals("USER_MANAGED", ext.publishingType.get())
        assertEquals(emptyList(), ext.shaAlgorithms.get())
    }
}
