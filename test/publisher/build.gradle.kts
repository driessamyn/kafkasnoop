plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")

    // TOOD: remove
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(project(":lib:avro"))

    // TODO: remove
    implementation("com.github.avro-kotlin.avro4k:avro4k-core:1.6.+")
}

description = "Test publisher"
jib.to.image = "driessamyn/kafkasnoop-test"

tasks.jar {
    this.archiveBaseName.set("kafkasnoop-test")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.test.AppKt")
}
