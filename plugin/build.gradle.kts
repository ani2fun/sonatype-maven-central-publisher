import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
    signing
    id("com.gradle.plugin-publish") version "1.2.1"
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
