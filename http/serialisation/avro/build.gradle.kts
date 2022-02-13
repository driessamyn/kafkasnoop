plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:avro"))
    implementation(project(":lib:http"))
}

jib.to.image = "driessamyn/kafkasnoop-avro"

tasks.jar {
    this.archiveBaseName.set("kafkasnoop.serialisation.avro")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.serialisation.avro.AppKt")
}
