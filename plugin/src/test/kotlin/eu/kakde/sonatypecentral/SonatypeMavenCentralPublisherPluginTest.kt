package eu.kakde.sonatypecentral

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    @Test fun `applying plugin succeeds when sourcesJar already exists`() {
        // Regression test for #5: another plugin (e.g. kotlin-jvm) may have
        // already registered a `sourcesJar` task of type org.gradle.jvm.tasks.Jar
        // before our plugin is applied. The previous code unconditionally called
        // javaPluginExtension.withSourcesJar(), which threw
        //   InvalidUserDataException: The task 'sourcesJar' (org.gradle.jvm.tasks.Jar)
        //   is not a subclass of the given type (org.gradle.api.tasks.bundling.Jar).
        // Now we guard the call.
        val project = ProjectBuilder.builder().withName("test").build()
        project.group = "com.example"
        project.version = "1.0"

        // Simulate another plugin's pre-existing legacy-typed jars.
        project.tasks.register("sourcesJar", org.gradle.jvm.tasks.Jar::class.java)
        project.tasks.register("javadocJar", org.gradle.jvm.tasks.Jar::class.java)

        project.plugins.apply("eu.kakde.gradle.sonatype-maven-central-publisher")
        (project as ProjectInternal).evaluate()

        // No exception thrown means the guard kicked in. The pre-existing tasks remain.
        assertNotNull(project.tasks.findByName("sourcesJar"))
        assertNotNull(project.tasks.findByName("javadocJar"))
    }
}
