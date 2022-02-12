plugins {
    id("kafkasnoop.kotlin-http-service-conventions")
    id("com.google.cloud.tools.jib")
}

repositories {
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.3-beta.2")
}

jib.to.image = "driessamyn/kafkasnoop"

tasks.jar {
    this.archiveBaseName.set("kafkasnoop")
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.AppKt")
}
