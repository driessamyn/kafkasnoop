package kafkasnoop.avro

import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class SchemaRegistry(schemas: Collection<Schema>) {
    companion object {
        private val logger = LoggerFactory.getLogger(SchemaRegistry::class.java)
        const val DEFAULT_FINGERPRINT_ALGORITHM = "SHA-256"
        private val encoder: Base64.Encoder = Base64.getEncoder()
    }

    val failedToParse = mutableListOf<AvroSchema>()
    private val schemasMap: Map<String, Schema>
    private val fingerPrintsToSchema = ConcurrentHashMap<String, Map<String, String>>()
    private val schemaToFingerprint = ConcurrentHashMap<String, Map<String, String>>()

    init {
        schemasMap = schemas.associateBy { it.fullName }
    }

    val all: Collection<Schema>
        get() = schemasMap.values

    fun getBase64FingerPrintFor(schemaName: String, algoName: String = "SHA-256"): String? {
        return schemaToFingerprint.computeIfAbsent(algoName) {
            val cache = fingerPrintsToSchema.computeIfAbsent(algoName) {
                computeFingerPrints(algoName)
            }
            cache.map { it.value to it.key }.toMap()
        }[schemaName]
    }

    fun getByName(name: String): Schema? {
        return schemasMap[name]
    }

    fun getByFingerPrint(print: ByteArray, algoName: String = DEFAULT_FINGERPRINT_ALGORITHM): Schema? {
        val cache = fingerPrintsToSchema.computeIfAbsent(algoName) {
            computeFingerPrints(algoName)
        }
        val name = cache[encoder.encodeToString(print)] ?: return null

        return schemasMap[name]
    }

    private fun computeFingerPrints(algoName: String): Map<String, String> {
        return schemasMap.values.map { schema ->
            try {
                encoder.encodeToString(SchemaNormalization.parsingFingerprint(algoName, schema)) to schema.fullName
            } catch (e: Exception) {
                logger.warn("Cannot encode fingerprint for ${schema.fullName}: $e")
                null
            }
        }.filterNotNull().toMap()
    }

    fun getDeserialiser(): Deserialiser {
        return Deserialiser(this)
    }

    fun getEnvelopeDeserialser(): EnvelopeDeserialiser {
        return EnvelopeDeserialiser(this)
    }
}
