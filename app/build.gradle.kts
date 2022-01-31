plugins {
    id("kafkasnoop.kotlin-application-conventions")
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.4.+")

    implementation("io.ktor:ktor-server-core:1.6.+")
    implementation("io.ktor:ktor-server-netty:1.6.+")

    implementation("org.apache.kafka:kafka-clients:3.1.+")
    implementation("org.apache.kafka:kafka-streams:3.1.+")

    implementation(project(":avro"))
}

application {
    // Define the main class for the application.
    mainClass.set("kafkasnoop.AppKt")
}
