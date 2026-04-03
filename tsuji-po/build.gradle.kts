plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
}

dependencies {
    // PO file parsing library
    implementation(libs.jgettext)

    // Logging
    implementation(libs.slf4j.api)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
