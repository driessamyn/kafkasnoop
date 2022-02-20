plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("kafkasnoop.kotlin-common-conventions")

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.4.+")

    implementation("io.ktor:ktor-server-core:1.6.+")
    implementation("io.ktor:ktor-server-netty:1.6.+")
    implementation("io.ktor:ktor-websockets:1.6.+")
    implementation("io.ktor:ktor-gson:1.6.+")

    implementation("io.ktor:ktor-client-core:1.6.+")
    implementation("io.ktor:ktor-client-java:1.6.+")

    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.3-beta.2")

    implementation("org.apache.kafka:kafka-clients:3.1.+")
    implementation("org.apache.kafka:kafka-streams:3.1.+")

    testImplementation("io.ktor:ktor-server-test-host:1.6.+")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.+")

    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")
}
