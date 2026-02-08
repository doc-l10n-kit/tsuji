pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}
rootProject.name = "tsuji"

include("tsuji-cli")
include("tsuji-tmx")