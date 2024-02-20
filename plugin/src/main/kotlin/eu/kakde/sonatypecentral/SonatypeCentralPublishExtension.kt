package eu.kakde.sonatypecentral

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.publish.VersionMappingStrategy
import org.gradle.api.publish.maven.MavenPom
import javax.inject.Inject

open class SonatypeCentralPublishExtension
    @Inject
    constructor(objectFactory: ObjectFactory) {
        val groupId: Property<String> = objectFactory.property(String::class.java)
        val artifactId: Property<String> = objectFactory.property(String::class.java)
        val version: Property<String> = objectFactory.property(String::class.java)
        val publishingType: Property<String> = objectFactory.property(String::class.java)
        val componentType: Property<String> = objectFactory.property(String::class.java)

        val username: Property<String> = objectFactory.property(String::class.java)
        val password: Property<String> = objectFactory.property(String::class.java)

        val pomConfiguration: Property<Action<MavenPom>> =
            objectFactory.property(Action::class.java) as Property<Action<MavenPom>>

        fun pom(action: Action<MavenPom>) {
            pomConfiguration.set(action)
        }

        var versionMappingStrategy: Action<VersionMappingStrategy>? = null

        fun versionMapping(action: Action<VersionMappingStrategy>) {
            this.versionMappingStrategy = action
        }

        companion object {
            internal fun Project.toSonatypeExtension(): SonatypeCentralPublishExtension =
                extensions.create("sonatypeCentralPublishExtension", SonatypeCentralPublishExtension::class.java)
        }
    }
