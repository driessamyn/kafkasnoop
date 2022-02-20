package kafkasnoop.serialisation

interface MessageDeserialiser {
    suspend fun deserialise(payload: ByteArray): String
}
