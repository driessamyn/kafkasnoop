plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":http:serialisation:avro"))
    implementation(project(":http:snoop"))
    implementation(project(":lib:avro"))
    implementation(project(":lib:http"))
}

// version of KafkaSnoop that wraps the snooper and the deserialiser into one HTTP server.
jib.to.image = "driessamyn/kafkasnoop-wrap"

tasks.jar {
    this.archiveBaseName.set("kafkasnoop-wrap")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.wrap.AppKt")
}
