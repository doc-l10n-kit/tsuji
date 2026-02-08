plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
}

dependencies {
    implementation(libs.jackson3.dataformat.xml)
    implementation(libs.jackson3.module.kotlin)
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}