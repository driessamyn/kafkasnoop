rootProject.name = "kafkasnoop"

pluginManagement {
    plugins {
        id("com.google.cloud.tools.jib") version "3.2.0"

        // TODO: remove
        id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    }
    repositories {
        gradlePluginPortal()
    }
}

include(
    "lib:avro",
    "lib:http",

    "http:serialisation:avro",
    "http:snoop",
    "http:snoop-wrap",

    "test:publisher",
)
