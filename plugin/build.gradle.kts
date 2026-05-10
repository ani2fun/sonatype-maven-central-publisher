import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.pluginPublish)
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
    signing
}

group = "eu.kakde.gradle"
version = "1.0.6"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        // Restrict the Java API surface to JDK 8 so post-8 methods (e.g. List.addLast)
        // fail at compile time instead of NoSuchMethodError on consumer JVMs.
        freeCompilerArgs.add("-Xjdk-release=1.8")
    }
}

java {
    withJavadocJar()
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(libs.gson)
    implementation(libs.okio)
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    testImplementation(libs.okhttpMockwebserver)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(libs.versions.kotlinVersion.get())
        }
    }
}

gradlePlugin {
    website = "https://github.com/ani2fun/sonatype-maven-central-publisher.git"
    vcsUrl = "https://github.com/ani2fun/sonatype-maven-central-publisher.git"

    val sonatypeMavenCentralPublish by plugins.creating {
        id = "eu.kakde.gradle.sonatype-maven-central-publisher"
        version = project.version
        implementationClass = "eu.kakde.sonatypecentral.SonatypeMavenCentralPublisherPlugin"
        displayName = "Sonatype Maven Central Repository Publisher"
        description = "Gradle plugin for building and uploading bundles to the Sonatype Maven Central Repository."
        tags = listOf("maven", "maven-central", "publish", "sonatype")
    }
}
