import net.sharplab.tsuji.gradle.VersionUtils

val tsujiVersion: String by project
val isSnapshot: String by project
val effectiveVersion = VersionUtils.getEffectiveVersion(isSnapshot.toBoolean(), tsujiVersion)

plugins {
    alias(libs.plugins.quarkus) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.allopen) apply false
}

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
