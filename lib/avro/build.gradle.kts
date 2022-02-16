plugins {
    id("kafkasnoop.kotlin-library-conventions")
}

dependencies {
    api("org.apache.avro:avro:1.11.+")
    api("com.google.code.gson:gson:2.9.+")

    testImplementation("com.google.jimfs:jimfs:1.+")
}
