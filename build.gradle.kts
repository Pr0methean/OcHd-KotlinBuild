import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "org.example"
version = "1.0-SNAPSHOT"
tasks.jar {
    manifest {
        attributes("Main-Class" to "io.github.pr0methean.ochd.MainKt")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from (
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.graphics", "javafx.swing")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-codec:1.14")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    testImplementation(kotlin("test"))
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
    implementation("guru.nidi.com.kitfox:svgSalamander:1.1.3")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.testfx:openjfx-monocle:jdk-11+26")
    implementation("org.apache.xmlgraphics:batik-all:1.14")
    runtimeOnly(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
