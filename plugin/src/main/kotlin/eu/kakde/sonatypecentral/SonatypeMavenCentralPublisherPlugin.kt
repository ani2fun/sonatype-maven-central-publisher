package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.SonatypeCentralPublishExtension.Companion.toSonatypeExtension
import eu.kakde.sonatypecentral.api.ArtifactCoordinates
import eu.kakde.sonatypecentral.api.BundleLayout
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.catalog.VersionCatalogPlugin
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningPlugin

const val CUSTOM_TASK_GROUP = "Publish to Sonatype Central"
const val HYPHEN_CHARACTER = "-"

class SonatypeMavenCentralPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Applying Necessary Plugins
        applyPlugins(project)

        // Configure Custom Extension
        println("Configuring SonatypeCentralPublishExtension...")
        val customExtension = project.toSonatypeExtension()

        // MAIN EXECUTION
        execution(project, customExtension)
    }
}

private fun applyPlugins(project: Project) {
    println("Applying java-library, maven-publish, signing and version-catalog plugins...")
    project.pluginManager.apply(JavaLibraryPlugin::class.java)
    project.pluginManager.apply(MavenPublishPlugin::class.java)
    project.pluginManager.apply(SigningPlugin::class.java)
    project.pluginManager.apply(VersionCatalogPlugin::class.java)
}

private fun execution(
    project: Project,
    extension: SonatypeCentralPublishExtension,
) {
    // Get Java Plugin Extension
    val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
    // Get Publication Container via Publishing Extension
    val publicationContainer = project.extensions.getByType(PublishingExtension::class.java).publications

    project.afterEvaluate {
        // Retrieve properties from custom extension
        val groupId = extension.groupId.get()
        val artifactId = extension.artifactId.get()
        val version = extension.version.get()
        val componentType = extension.componentType.get() // Component Type. Either "java" or "versionCatalog"
        val shaAlgorithms = extension.shaAlgorithms.get()
        println("Configuring details - Group ID: $groupId, Artifact ID: $artifactId, Version: $version, Component Type: $componentType")

        // In-built plugin call to get javadoc and sources. Skip if another plugin
        // (e.g. kotlin-jvm) already registered a task with the same name —
        // calling withSourcesJar()/withJavadocJar() in that case throws
        // InvalidUserDataException about a Jar type mismatch.
        if (project.tasks.findByName("sourcesJar") == null) javaPluginExtension.withSourcesJar()
        if (project.tasks.findByName("javadocJar") == null) javaPluginExtension.withJavadocJar()

        // Prepare Maven Publication
        val mavenPublication = prepareMavenPublication(extension, publicationContainer, project)

        registerTasks(
            project = project,
            mavenPublication = mavenPublication,
            componentType = componentType,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            shaAlgorithms = shaAlgorithms,
        )
    }
}

// Register tasks for the plugin
private fun registerTasks(
    project: Project,
    mavenPublication: MavenPublication,
    componentType: String?,
    groupId: String,
    artifactId: String,
    version: String,
    shaAlgorithms: List<String>,
) {
    // One source of truth for where the bundle stages, where the zip lands, and
    // what coordinates the bundle represents. Shared by the four file/network tasks below.
    val layout =
        BundleLayout.from(
            buildDirectory = project.layout.buildDirectory.get().asFile,
            coordinates = ArtifactCoordinates(groupId, artifactId, version),
        )

    project.tasks.register("generateMavenArtifacts", GenerateMavenArtifacts::class.java, componentType)
    project.tasks.register("signMavenArtifacts", SignMavenArtifact::class.java, mavenPublication)
    project.tasks.register("aggregateFiles", AggregateFiles::class.java, layout)
    project.tasks.register("computeHash", ComputeHash::class.java, layout, shaAlgorithms)
    project.tasks.register("createZip", CreateZip::class.java, layout)
    project.tasks.register("publishToSonatype", PublishToSonatypeCentral::class.java, layout)
    project.tasks.register("getDeploymentStatus", GetDeploymentStatus::class.java)
    project.tasks.register("dropDeployment", DropDeployment::class.java)
}

private fun prepareMavenPublication(
    customExtension: SonatypeCentralPublishExtension,
    publicationContainer: PublicationContainer,
    project: Project,
): MavenPublication {
    val componentType = customExtension.componentType.get()
    val groupId = customExtension.groupId.get()
    val artifactId = customExtension.artifactId.get()
    val version = customExtension.version.get()
    val softwareComponent = project.components.getByName(componentType ?: "java")

    val mavenPublication =
        publicationContainer.create("maven", MavenPublication::class.java) { publication ->
            publication.from(softwareComponent)
            publication.groupId = groupId
            publication.artifactId = artifactId
            publication.version = version

            customExtension.pomConfiguration.orNull?.let { action ->
                publication.pom(action)
            }

            customExtension.versionMappingStrategy?.let {
                publication.versionMapping(it)
            }
            // add jar tasks
            val jarTasks: List<Task> = initJarTasks(project, componentType, artifactId, version)
            println("Adding ${jarTasks.size} jar task into the maven publication.")
            jarTasks.forEach { task ->
                val artifact: MavenArtifact = publication.artifact(task)
                publication.artifacts.add(artifact)
            }
        }

    return mavenPublication
}

private fun initJarTasks(
    project: Project,
    componentType: String,
    artifactId: String,
    version: String,
): List<Task> {
    // Return these jar tasks to maven publication
    val jarTaskList = mutableListOf<Task>()

    val commonJarTaskConfig =
        listOf(
            JarTaskConfig("sourcesJar", artifactId, version),
            JarTaskConfig("javadocJar", artifactId, version),
        )

    val jarTaskConfigs =
        when (componentType) {
            "java" -> {
                val additionalTask =
                    if (project.hasProperty("bootJar")) {
                        JarTaskConfig("bootJar", artifactId, version)
                    } else {
                        JarTaskConfig("jar", artifactId, version)
                    }
                commonJarTaskConfig + additionalTask
            }

            "versionCatalog" -> commonJarTaskConfig
            else -> emptyList()
        }

    jarTaskConfigs.forEach { config ->
        val taskName = config.taskName
        val task: Jar? = project.tasks.findByName(taskName) as? Jar

        task?.let {
            with(it) {
                group = CUSTOM_TASK_GROUP
                description = "The task is about $taskName."

                val fileName = archiveFileName.get()
                val archiveSuffix: String =
                    when {
                        fileName.endsWith("-sources.jar") -> "-sources.jar"
                        fileName.endsWith("-javadoc.jar") -> "-javadoc.jar"
                        else -> ".jar"
                    }
                it.archiveFileName.set("$artifactId$HYPHEN_CHARACTER$version$archiveSuffix")
            }

            jarTaskList.add(it)
        }
    }

    return jarTaskList
}

data class JarTaskConfig(
    val taskName: String,
    val artifactId: String,
    val version: String,
)
