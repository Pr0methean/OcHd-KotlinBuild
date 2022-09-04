import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.10"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("io.gitlab.arturbosch.detekt").version("1.21.0")
}

group = "org.example"
version = "1.0-SNAPSHOT"

val mainClassName = "io.github.pr0methean.ochd.MainKt"

application {
    mainClass.set(mainClassName)
}

tasks.jar {
    manifest {
        attributes("Main-Class" to mainClassName, "Multi-Release" to true)
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from (
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
}

javafx {
    version = "18.0.1"
    modules = listOf("javafx.graphics", "javafx.swing")
}

repositories {
    mavenCentral()
}

val log4jVersion = "2.18.0"

dependencies {
    testImplementation(kotlin("test"))
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.6.4")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.testfx:openjfx-monocle:jdk-12.0.1+2")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    implementation("org.apache.xmlgraphics:batik-codec:1.14")
    runtimeOnly(kotlin("reflect"))
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-iostreams:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-jul:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-jpl:$log4jVersion")
    runtimeOnly("com.lmax:disruptor:3.4.4")
    constraints {
        add("detekt", "org.yaml:snakeyaml") {
            version {
                require("1.31")
            }
            because("CVE-2022-25857")
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
    kotlinOptions.jvmTarget = "18"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
