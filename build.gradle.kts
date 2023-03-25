import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.8.10"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("org.sonarqube") version "4.0.0.2929"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val jdkVersion: String = "19"
val myMainClassName: String = "io.github.pr0methean.ochd.MainKt"

application {
    mainClass.set(myMainClassName)
}

tasks.jar {
    manifest {
        attributes("Main-Class" to myMainClassName, "Multi-Release" to true)
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from (
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
}

javafx {
    version = jdkVersion
    modules = listOf("javafx.graphics", "javafx.swing")
}

repositories {
    mavenCentral()
}

val log4jVersion: String = "2.19.0"
val kotlinXCoroutinesVersion: String = "1.6.4"
val batikVersion: String = "1.16"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinXCoroutinesVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$kotlinXCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$kotlinXCoroutinesVersion")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.testfx:openjfx-monocle:jdk-12.0.1+2")
    implementation("org.apache.xmlgraphics:batik-transcoder:$batikVersion")
    implementation("org.apache.xmlgraphics:batik-codec:$batikVersion")
    runtimeOnly(kotlin("reflect"))
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4jVersion")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    runtimeOnly("com.lmax:disruptor:3.4.4")
    constraints {
        add("detekt", "org.yaml:snakeyaml") {
            version {
                require("2.0")
            }
            because("""
                CVE-2022-25857, 
                CVE-2022-38749, 
                CVE-2022-38750, 
                CVE-2022-38751, 
                CVE-2022-38752,
                CVE-2022-1471""".trimIndent())
        }
        add("implementation", "commons-io:commons-io") {
            version {
                require("2.7")
            }
            because("CVE-2021-29425")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.languageVersion = "1.9"
    kotlinOptions.jvmTarget = jdkVersion
    kotlinOptions.useK2 = true
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion.toInt()))
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

sonarqube {
    properties {
        property("sonar.projectKey", "Pr0methean_OcHd-KotlinBuild")
        property("sonar.organization", "pr0methean")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
