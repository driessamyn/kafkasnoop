plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("kafkasnoop.kotlin-common-conventions")

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

dependencies {
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")
}
