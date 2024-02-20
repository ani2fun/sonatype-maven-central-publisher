package eu.kakde.sonatypecentral

import eu.kakde.sonatypecentral.SonatypeCentralPublishExtension.Companion.toSonatypeExtension
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
import java.io.File
import java.util.Locale

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
        println("Configuring details - Group ID: $groupId, Artifact ID: $artifactId, Version: $version, Component Type: $componentType")

        // In-built plugin call to get javadoc and sources
        javaPluginExtension.withSourcesJar()
        javaPluginExtension.withJavadocJar()

        // Prepare Maven Publication
        val mavenPublication = prepareMavenPublication(extension, publicationContainer, project)

        registerTasks(
            project = project,
            mavenPublication = mavenPublication,
            componentType = componentType,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
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
) {
    // Generate Maven Artifact task
    project.tasks.register("generateMavenArtifacts", GenerateMavenArtifacts::class.java, componentType)
    // Signed Maven Artifact task
    project.tasks.register("signMavenArtifacts", SignMavenArtifact::class.java, mavenPublication)

    // Create the necessary directory structure to aggregate publications at a specific location for the Zip task.
    val buildDir = project.layout.buildDirectory.get().asFile.resolve("upload")
    val namespacePath = groupId.replace('.', File.separatorChar)
    val directoryPath = "${buildDir.path}/$namespacePath/$artifactId/$version"
    val aggregateFiles = project.tasks.register("aggregateFiles", AggregateFiles::class.java)
    aggregateFiles.configure {
        it.directoryPath = directoryPath
        it.groupId = groupId
        it.artifactId = artifactId
        it.version = version
    }

    // Calculate md5 and sha1 hash of all files in a given directory
    project.tasks.register("computeHash", ComputeHash::class.java, File(directoryPath))
    // Create a zip of all files in a given directory
    val createZip = project.tasks.register("createZip", CreateZip::class.java)
    createZip.configure { it.folderPath = project.layout.buildDirectory.get().asFile.resolve("upload").path }

    // Publish to Sonatype Maven Central Repository
    project.tasks.register("publishToSonatype", PublishToSonatypeCentral::class.java)

    // Get the deployment status of published deployment by deploymentId
    project.tasks.register("getDeploymentStatus", GetDeploymentStatus::class.java)

    // Drop a deployment by deploymentId
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

            customExtension.pomConfiguration.let {
                publication.pom(it.get())
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
            "java" -> commonJarTaskConfig + JarTaskConfig("jar", artifactId, version)
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
                val archiveSuffix: String =
                    if (taskName != "jar") {
                        val cleanName = taskName.removeSuffix("Jar").lowercase(Locale.getDefault())
                        "$HYPHEN_CHARACTER$cleanName.jar"
                    } else {
                        ".jar"
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
