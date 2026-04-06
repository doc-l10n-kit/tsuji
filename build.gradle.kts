import net.sharplab.tsuji.gradle.VersionUtils
import org.gradle.api.tasks.testing.Test

val tsujiVersion: String by project
val isSnapshot: String by project
val effectiveVersion = VersionUtils.getEffectiveVersion(isSnapshot.toBoolean(), tsujiVersion)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.quarkus)
}

// Project metadata
group = "net.sharplab.tsuji"
version = effectiveVersion

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    group = "net.sharplab.tsuji"
    version = effectiveVersion

    // Java version configuration
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    // Kotlin compilation configuration
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                javaParameters.set(true)
            }
        }
    }

    // Common test configuration
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

// Root project (tsuji CLI application) configuration
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val systemTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project())
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                        forkEvery = 1
                        failFast = true
                    }
                }
            }
        }
    }
}

configurations {
    named("systemTestImplementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    named("systemTestRuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
}

dependencies {
    implementation(project(":tsuji-tmx"))
    implementation(project(":tsuji-po"))
    implementation(platform(libs.quarkus.bom))
    implementation(platform(libs.quarkus.langchain4j.bom))
    implementation(libs.quarkus.langchain4j.ai.gemini)
    implementation(libs.langchain4j.community.lucene)
    implementation(libs.langchain4j.embeddings.all.minilm.l6.v2.q)

    implementation(libs.quarkus.picocli)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.config.yaml)

    // Kotlin Coroutines for adaptive parallelism control
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")

    implementation(libs.asciidoctorj)
    implementation(libs.jsoup)
    implementation(libs.deepl.java)
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.quarkus.junit5.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(libs.assertj.core)
}

quarkus {
    setFinalName("tsuji")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

// Tasks for version management
tasks.register("switchToSnapshot") {
    group = "release"
    doLast {
        val file = file("gradle.properties")
        val original = file.readText()
        val updated = original.replace(Regex("""^isSnapshot=.*$""", RegexOption.MULTILINE), "isSnapshot=true")
        file.writeText(updated)
    }
}

tasks.register("switchToRelease") {
    group = "release"
    doLast {
        val file = file("gradle.properties")
        val original = file.readText()
        val updated = original.replace(Regex("""^isSnapshot=.*$""", RegexOption.MULTILINE), "isSnapshot=false")
        file.writeText(updated)
    }
}

tasks.register("bumpPatchVersion") {
    group = "release"
    doLast {
        val nextVersion = VersionUtils.bumpPatchVersion(tsujiVersion)
        val file = file("gradle.properties")
        val original = file.readText()
        val updated = original.replace(Regex("""^tsujiVersion=.*$""", RegexOption.MULTILINE), "tsujiVersion=$nextVersion")
        file.writeText(updated)
    }
}

tasks.register("updateVersionsInDocuments") {
    group = "release"
    doLast {
        val file = file("README.md")
        if (file.exists()) {
            val original = file.readText()
            val updated = original.replace(Regex("""^Version: .*$""", RegexOption.MULTILINE), "Version: $effectiveVersion")
            file.writeText(updated)
            logger.lifecycle("Updated Version in README.md to $effectiveVersion")
        }
    }
}
