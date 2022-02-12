plugins {
    id("kafkasnoop.kotlin-library-conventions")
}

dependencies {
    implementation("org.apache.avro:avro:1.11.+")
    implementation("com.google.code.gson:gson:2.9.+")

    testImplementation("com.google.jimfs:jimfs:1.+")
}
