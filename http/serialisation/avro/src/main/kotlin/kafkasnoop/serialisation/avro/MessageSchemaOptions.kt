package kafkasnoop.serialisation.avro

interface MessageSchemaOptions {
    val schemaPath: String?
    val envelopeSchemaName: String?
    val schemaNameFieldInEnvelope: String?
    val payloadFieldInEnvelope: String?
    val schemaFingerPrintFieldInEnvelope: String?
    val schemaFingerPrintAlgorithmFieldInEnvelope: String?
    val schemaFingerPrintAlgorithm: String
}
