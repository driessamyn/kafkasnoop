plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:http"))
}

jib.to.image = "driessamyn/kafkasnoop"

tasks.jar {
    this.archiveBaseName.set("kafkasnoop")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.AppKt")
}
