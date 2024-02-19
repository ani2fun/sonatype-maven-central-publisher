# Plugin for publishing to sonatype maven central 

**Maven Central Publisher Plugin**

Description:
The Maven Central Publisher Plugin is a Gradle plugin designed to streamline the process of publishing artifacts to Maven Central. Currently, it supports publishing Java libraries and gradle version catalog files.
This plugin automates the necessary tasks involved in preparing and publishing artifacts, ensuring a smooth and efficient workflow for developers.


The Maven Central Publisher Plugin simplifies the process of publishing artifacts to Maven Central using Gradle. Follow these steps to integrate and configure the plugin for your project:

**1. Apply the Plugin**

Apply the plugin in your `build.gradle.kts` file:

```kotlin
plugins {
    id("eu.kakde.gradle.sonatype-maven-central-publisher") version "1.0.0"
}
```

**2. Configure GPG Signing**

Ensure that GPG signing is correctly configured for your project using the Gradle signing extension. You can find more information on signing publication for Gradle [here](https://docs.gradle.org/current/userguide/signing_plugin.html).

Below is sample configuration for `gradle.properties` file:

```properties
###
signing.keyId=ABCDEFGH
signing.password=your_password
signing.secretKeyRingFile=/Users/johndoe/.gnupg/secring.gpg
###
sonatypeUsername=your_sonatype_username
sonatypePassword=your_sonatype_password
###
```

Please note that the provided GPG configuration is used for testing the publishing of [samplelib](https://repo1.maven.org/maven2/eu/kakde/plugindemo/samplelib/) artifacts here and [samplecatalog](https://repo1.maven.org/maven2/eu/kakde/plugindemo/samplecatalog/) artifacts here on Maven Central.

**3. Configure Publication**

Add the `sonatypeCentralPublishExtension` to configure the publication:

```kotlin
val sonatypeUsername: String? by project // this is defined in ~/.gradle/gradle.properties
val sonatypePassword: String? by project // this is defined in ~/.gradle/gradle.properties

sonatypeCentralPublishExtension {
    // Set group ID, artifact ID, version, and other publication details
    groupId.set(Meta.GROUP)
    artifactId.set(Meta.ARTIFACT_ID)
    version.set(Meta.VERSION)
    componentType.set(Meta.COMPONENT_TYPE) // "java" or "versionCatalog"
    publishingType.set(Meta.PUBLISHING_TYPE) // USER_MANAGED or AUTOMATIC
    
    // Set username and password for Sonatype repository
    username.set(System.getenv("SONATYPE_USERNAME") ?: sonatypeUsername)
    password.set(System.getenv("SONATYPE_PASSWORD") ?: sonatypePassword)
    
    // Configure POM metadata
    pom {
        name.set(Meta.ARTIFACT_ID)
        description.set(Meta.DESC)
        url.set("https://github.com/${Meta.GITHUB_REPO}")
        licenses {
            license {
                name.set(Meta.LICENSE)
                url.set(Meta.LICENSE_URL)
            }
        }
        developers {
            developer {
                id.set(Meta.DEVELOPER_ID)
                name.set(Meta.DEVELOPER_NAME)
                organization.set(Meta.DEVELOPER_ORGANIZATION)
                organizationUrl.set(Meta.DEVELOPER_ORGANIZATION_URL)
            }
        }
        scm {
            url.set("https://github.com/${Meta.GITHUB_REPO}")
            connection.set("scm:git:https://github.com/${Meta.GITHUB_REPO}")
            developerConnection.set("scm:git:https://github.com/${Meta.GITHUB_REPO}")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/${Meta.GITHUB_REPO}/issues")
        }
    }
}

object Meta {
    const val COMPONENT_TYPE = "java" // "java" or "versionCatalog"

    const val GROUP = "eu.kakde.plugindemo"
    const val ARTIFACT_ID = "samplelib"
    const val VERSION = "1.0.0"
    const val PUBLISHING_TYPE = "AUTOMATIC" // USER_MANAGED or AUTOMATIC

    const val DESC = "GitHub Version Catalog Repository for Personal Projects based on Gradle"
    const val LICENSE = "Apache-2.0"
    const val LICENSE_URL = "https://opensource.org/licenses/Apache-2.0"
    const val GITHUB_REPO = "ani2fun/plugin-demo.git"
    const val DEVELOPER_ID = "ani2fun"
    const val DEVELOPER_NAME = "Aniket Kakde"
    const val DEVELOPER_ORGANIZATION = "kakde.eu"
    const val DEVELOPER_ORGANIZATION_URL = "https://www.kakde.eu"
}

```

**4. Configure Component Type**

Specify the component type as `"java"` for publishing a Java library or `"versionCatalog"` for publishing a version catalog.

**5. Testing and Configuration**

**6. Sample Projects**

Sample projects demonstrating Java library publishing and version catalog can be found at [plugin-demo](https://github.com/ani2fun/plugin-demo).

For more information and updates, visit the [Maven Central Publisher Plugin](https://github.com/ani2fun/plugin-demo).