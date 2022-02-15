package kafkasnoop.avro

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DecoderFactory
import org.slf4j.LoggerFactory

class Deserialiser(
    private val schemaRegistry: SchemaRegistry
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SchemaLoader::class.java)
    }

    val decoderFactory: DecoderFactory = DecoderFactory.get()

    fun decode(msg: ByteArray): JsonArray {
        val jsonArray = JsonArray()
        schemaRegistry.all.forEach { schema ->
            try {
                jsonArray.add(
                    decode(msg, schema.fullName)
                )
            } catch (e: Exception) {
                logger.debug("Cannot convert msg with ${schema.fullName} - ${e.message}")
            }
        }
        return jsonArray
    }

    fun decode(msg: ByteArray, schemaName: String): JsonObject {
        return JsonObject().apply {
            addProperty("schema", schemaName)
            add("message", decodeElement(msg, schemaName))
        }
    }

    private fun decodeElement(msg: ByteArray, schemaName: String): JsonElement {
        val schema = schemaRegistry.getByName(schemaName)
            ?: throw AvroSerialisationException("Could not find schema $schemaName")
        val datumReader: DatumReader<Any> = GenericDatumReader(schema)
        try {
            val decoder = decoderFactory.binaryDecoder(msg, null)
            val avroDatum = datumReader.read(null, decoder)
            return JsonParser.parseString(avroDatum.toString())
        } catch (e: Exception) {
            val eMsg = "Cannot convert msg with ${schema.fullName}"
            logger.warn("$eMsg - ${e.message}")
            throw AvroSerialisationException(eMsg, e)
        }
    }
}
