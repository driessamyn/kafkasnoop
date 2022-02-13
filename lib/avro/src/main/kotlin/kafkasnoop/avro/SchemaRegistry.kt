package kafkasnoop.avro

import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

class SchemaRegistry(schemas: Collection<Schema>) {
    companion object {
        private val encoder: Base64.Encoder = Base64.getEncoder()
    }

    private val schemasMap: Map<String, Schema>
    private val fingerPrintsToSchema = ConcurrentHashMap<String, Map<String, String>>()
    init {
        schemasMap = schemas.associateBy { it.fullName }
    }

    val all: Collection<Schema>
        get() = schemasMap.values

    fun getByName(name: String): Schema? {
        return schemasMap[name]
    }

    fun getByFingerPrint(print: ByteArray, fingerPrintName: String = "SHA-256"): Schema? {
        val name = fingerPrintsToSchema.computeIfAbsent(fingerPrintName) {
            schemasMap.values.map { schema ->
                encoder.encodeToString(SchemaNormalization.parsingFingerprint(fingerPrintName, schema)) to schema.fullName
            }.toMap()
        }[encoder.encodeToString(print)] ?: return null

        return schemasMap[name]
    }
}
