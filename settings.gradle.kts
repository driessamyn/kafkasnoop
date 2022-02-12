rootProject.name = "kafkasnoop"

pluginManagement {
    plugins {
        id("com.google.cloud.tools.jib") version "3.2.0"
    }
    repositories {
        gradlePluginPortal()
    }
}

include(
    "avro",
    "http:serialisation:avro",
    "http:snoop",
)
