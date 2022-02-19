package kafkasnoop.serialisation

class SimpleByteToStringDeserialiser : MessageDeserialiser {
    override suspend fun deserialise(payload: ByteArray): String {
        return String(payload, Charsets.UTF_8)
    }
}

