rootProject.name = "kafkasnoop"

pluginManagement {
    plugins {
        id("com.google.cloud.tools.jib") version "3.2.0"
    }
    repositories {
        gradlePluginPortal()
    }
}

include("http", "avro")
