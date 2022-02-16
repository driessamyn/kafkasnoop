package kafkasnoop.avro

import com.google.gson.JsonObject
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

/**
 * Deserialiser with support for AVRO Envelopes
 */
class EnvelopeDeserialiser(
    val schemaRegistry: SchemaRegistry,
    val avroDeserialiser: Deserialiser = Deserialiser(schemaRegistry)
) {
    companion object {
        private val logger = LoggerFactory.getLogger(EnvelopeDeserialiser::class.java)
    }
    val decoderFactory: DecoderFactory = DecoderFactory.get()

    /**
     * Decode [msg] using schema identified by the value of [schemaNameField] and binary
     * from the [messageField] value.
     */
    fun decode(
        msg: ByteArray,
        envelopeSchemaName: String,
        schemaNameField: String,
        messageField: String
    ): JsonObject {
        val envelope = decodeEnvelope(msg, envelopeSchemaName)
        val schemaName = envelope.getString(schemaNameField)
        val bytes = envelope.getByteArray(messageField)

        return avroDeserialiser.decode(bytes, schemaName)
    }

    /**
     * Decode [msg] using schema identified by the value of [schemaFingerPrintField] and binary
     * from the [messageField] value.
     * If [schemaFingerPrintAlgoritmField] is provided, fingerprint algorithm is part of the message
     * envelope, otherwise [schemaFingerPrintAlgorithm] will be used.
     */
    fun decodeFromFingerPrint(
        msg: ByteArray,
        envelopeSchemaName: String,
        schemaFingerPrintField: String,
        messageField: String,
        schemaFingerPrintAlgoritmField: String? = null,
        schemaFingerPrintAlgorithm: String = "SHA-256"
    ): JsonObject {
        val envelope = decodeEnvelope(msg, envelopeSchemaName)
        val schemaFingerPrint = envelope.getByteArray(schemaFingerPrintField)
        val bytes = envelope.getByteArray(messageField)

        val fingerPrintAlgo = if (null != schemaFingerPrintAlgoritmField)
            envelope.getString(schemaFingerPrintAlgoritmField) else schemaFingerPrintAlgorithm

        val schemaName =
            schemaRegistry.getByFingerPrint(schemaFingerPrint, fingerPrintAlgo)?.fullName
                ?: throw AvroSerialisationException("Cannot find schema with fingerprint: $schemaFingerPrint")
        return avroDeserialiser.decode(bytes, schemaName)
    }

    private fun decodeEnvelope(msg: ByteArray, schemaName: String): GenericData.Record {
        val schema = schemaRegistry.getByName(schemaName)
            ?: throw AvroSerialisationException("Could not find schema $schemaName")
        val datumReader: DatumReader<Any> = GenericDatumReader(schema)
        try {
            val decoder = decoderFactory.binaryDecoder(msg, null)
            return datumReader.read(null, decoder) as GenericData.Record
        } catch (e: Exception) {
            val eMsg = "Cannot convert msg envelope with ${schema.fullName}"
            logger.warn("$eMsg - ${e.message}")
            throw AvroSerialisationException(eMsg, e)
        }
    }
}

private fun GenericData.Record.getString(field: String): String {
    return this.get(field).toString()
}

private fun GenericData.Record.getByteArray(field: String): ByteArray {
    val record = this.get(field)
    if (record is GenericData.Fixed)
        return record.bytes()
    else if (record is ByteBuffer)
        return record.array()
    throw AvroSerialisationException("Cannot conver type $record")
}
