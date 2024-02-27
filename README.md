# Gradle Plugin for publishing to [central sonatype maven repository](https://central.sonatype.com/)

The Maven Central Publisher Plugin is a Gradle plugin designed to streamline the process of publishing artifacts to
Maven Central repository. Currently, it supports publishing Java libraries, gradle version catalog files and spring boot application.

**Sample projects illustrating the publishing of Java libraries, version catalogs, and Spring Boot applications using this plugin can be accessed at: https://github.com/ani2fun/plugin-demo**

> [!NOTE]
> This project serves as a temporary solution. As a Gradle user, I found a gap in the existing plugins suited to my requirements at the time.
> It encourage me to develop a custom solution to publish various artifacts to sonatype central repository, which
> includes Java libraries and Spring Boot applications and Gradle Version-Catalog.
> Looking ahead, more efficient solutions will be arriving, either through direct enhancements from platform developers
> or Gradle engineers.
> This project presents a valuable opportunity to learn Gradle plugin development

Follow these steps to integrate and configure the plugin for your project:

## 1. Apply the Plugin

Apply the plugin in your `build.gradle.kts` file:

```kotlin
plugins {
    id("eu.kakde.gradle.sonatype-maven-central-publisher") version "1.0.3"
}
```

## 2. Configure GPG Signing

Ensure that GPG signing is correctly configured for your project using the Gradle signing extension. You can find more
information on signing publication for Gradle [here](https://docs.gradle.org/current/userguide/signing_plugin.html).

Below is sample configuration for `gradle.properties` file:

```properties
##############################
# GPG Credentials
# Last 8 characters in your GPG public key
signing.keyId=<ABCD1234>
# Your signing passphrase
signing.password=<gpg_passphrase>
# private key secring.gpg file path: /Users/jondoe/.gnupg/secring.gpg
signing.secretKeyRingFile=<file_path>

# sonatype credentials
sonatypeUsername=<username>
sonatypePassword=<password>
##############################
```

Please note that the GPG configuration format mentioned above is utilized for testing the publication to the new Maven Central repository, covering the following artifacts:
- **Example Java library:    https://repo1.maven.org/maven2/eu/kakde/plugindemo/samplelib/**
- **Example Version Catalog: https://repo1.maven.org/maven2/eu/kakde/plugindemo/samplecatalog/**
- **Example SpringBoot App:  https://repo1.maven.org/maven2/eu/kakde/plugindemo/springbootapp/**

## 3. Configure Publication

Add the `sonatypeCentralPublishExtension` to configure the publication:

> [!NOTE]
> Specify the component type as "java" for publishing Java artifacts (which includes Java libraries or Spring Boot applications), or "versionCatalog" for publishing a version catalog.

The sample configuration block in `build.gradle.kts` appears as follows:

```kotlin
// ------------------------------------
// PUBLISHING TO SONATYPE CONFIGURATION
// ------------------------------------
object Meta { 
  val COMPONENT_TYPE = "java" // "java" or "versionCatalog"
  val GROUP = "eu.kakde.plugindemo"
  val ARTIFACT_ID = "samplelib"
  val VERSION = "1.0.0"
  val PUBLISHING_TYPE = "AUTOMATIC" // USER_MANAGED or AUTOMATIC
  val SHA_ALGORITHMS = listOf("SHA-256", "SHA-512") // sha256 and sha512 are supported but not mandatory. Only sha1 is mandatory but it is supported by default.
  val DESC = "GitHub Version Catalog Repository for Personal Projects based on Gradle"
  val LICENSE = "Apache-2.0"
  val LICENSE_URL = "https://opensource.org/licenses/Apache-2.0"
  val GITHUB_REPO = "ani2fun/plugin-demo.git"
  val DEVELOPER_ID = "ani2fun"
  val DEVELOPER_NAME = "Aniket Kakde"
  val DEVELOPER_ORGANIZATION = "kakde.eu"
  val DEVELOPER_ORGANIZATION_URL = "https://www.kakde.eu"
}

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

```

## 5. Finally, to Publish your Projects to Sonatype Central

Follow these steps:

- Let's say you have project called `plugin-demo`, then to publish to sonatype central you need to execute task
  called `publishToSonatype`

  For example:
    ```bash
    ./gradlew :plugin-demo:clean :plugin-demo:publishToSonatype
    ```

  It will generate a publication in the build directory of your project and create a zip file named upload.zip. This
  file will be published to Maven Central. If you have utilized the "USER_MANAGED" option
  in `sonatypeCentralPublishExtension` block, please navigate to your deployment in your Maven account and click "
  publish" to finalize the publication process

- After publishing to Maven Central, you'll see a Deployment ID in the console or the error message if something goes
  wrong:
  Success message:
    ```console
    Deployment Response: "95a20ab9-d2f8-49ee-9101-19aba493a730"
    ```
  OR
  Error message:
    ```console
     > Task :plugin:publishToSonatype
     Executing 'publishToSonatypeCentral' tasks...
     Cannot publish to Maven Central (status='400'). Deployment ID='{"error":{"message":"Wrong token"}}'
    ```

- To check the deployment status, use the following task:

  ```bash
  ./gradlew :plugin-demo:getDeploymentStatus -PdeploymentId=1c28f4ad-4a88-4662-89e6-49a51484ffb1
  ```

You'll see the deployment response in the console:

  ```console
  > Task :plugin-demo:getDeploymentStatus
  Executing 'getDeploymentStatus' task... With parameter deploymentId=1c28f4ad-4a88-4662-89e6-49a51484ffb1
  Deployment Response:
  {
    "deploymentId": "1c28f4ad-4a88-4662-89e6-49a51484ffb1",
    "deploymentName": "eu.kakde.plugindemo:samplelib:1.0.0",
    "deploymentState": "PUBLISHED",
    "purls": [
      "pkg:maven/eu.kakde.plugindemo/samplelib@1.0.0?type=pom"
    ],
    "errors": {
      "common": [
        "Deployment components info not found"
      ]
    },
    "cherryBomUrl": "https://sbom.sonatype.com/report/T2-1708358340-575a0a7950c749d8841712efd60107d6"
  }
  ```

- If you have used `publishingType=USER_MANAGED` and you wish to drop the deployment using the deployment ID, use the
  following task:
  ```bash
  ./gradlew :plugin-demo:dropDeployment -PdeploymentId=<deployement-ID>
  ```

- To view the tasks implemented in this plugin, execute the following command:
    ```bash
    > ./gradlew :plugin-demo:tasks
    
    Publish to Sonatype Central tasks
    ---------------------------------
    publishToSonatype - Publish to New Sonatype Maven Central Repository.
    generateMavenArtifacts - Generates all necessary artifacts for maven publication.
    signMavenArtifacts - Sign necessary artifacts generated by 'generateMavenArtifacts'.
    aggregateFiles - Aggregate all files to a temporary directory.
    computeHash - Compute Hash of all files in a temporary directory.
    createZip - Create a zip file comprising all files located within a temporary directory
    getDeploymentStatus - Get deployment status using deploymentId.
    dropDeployment - Drop deployment using deploymentId.
    ```

### Side note:

You can test the plugin locally by publishing it and then importing it into the corresponding project. Run the following
command to publish the plugin to your local Maven repository:

```bash
./gradlew :plugin:publishToMavenLocal
```

This command will publish the plugin to your local Maven repository, making it available for use in other projects on
your local environment using the same way as in `1.Apply the Plugin` section.
