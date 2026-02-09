import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.quarkus)
}

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
    implementation(platform(libs.quarkus.bom))
    implementation(platform(libs.quarkus.langchain4j.bom))
    implementation(libs.quarkus.langchain4j.ai.gemini)
    implementation(libs.langchain4j.community.lucene)
    implementation(libs.langchain4j.embeddings.all.minilm.l6.v2.q)

    implementation(libs.quarkus.picocli)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.arc)

    implementation(libs.jgettext)
    implementation(libs.asciidoctorj)
    implementation(libs.jsoup)
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