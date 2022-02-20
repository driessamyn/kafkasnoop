plugins {
    id("kafkasnoop.kotlin-library-conventions")
}

dependencies {
    // TODO: version catalogue

    implementation("com.github.ajalt.clikt:clikt:3.4.+")
    implementation("io.ktor:ktor-server-core:1.6.+")
    implementation("io.ktor:ktor-gson:1.6.+")
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.3-beta.2")
}
