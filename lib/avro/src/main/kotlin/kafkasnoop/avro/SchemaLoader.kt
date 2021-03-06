package kafkasnoop.avro

import org.apache.avro.Schema
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.streams.toList

class SchemaLoader(
    private val schemaRegistryFactory: SchemaRegistryFactory = SchemaRegistryFactory()
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SchemaLoader::class.java)
    }

    /**
     * Create a [SchemaRegistry] by scanning a directory for AVRO schemas.
     *
     * @param path directory path to scan (recursively).
     */
    fun createFromDir(path: Path): SchemaRegistry {
        val files = Files.walk(path)
            .filter { it.name.endsWith("avsc") }
            .toList()
        return createFromSchemaFiles(files)
    }

    /**
     * Create a [SchemaRegistry] by reading AVRO schemas from given [files].
     *
     * @param files containing AVRO Schemas in JSON format.
     */
    fun createFromSchemaFiles(files: List<Path>): SchemaRegistry {
        return createFromSchemaSources(files.map { Files.readString(it) })
    }

    /**
     * Create a [SchemaRegistry] from schema definitions given in  [schemas].
     *
     * @param schemas in JSON format.
     */
    fun createFromSchemaSources(schemas: List<String>): SchemaRegistry {
        val all = schemas.map {
            try {
                val schema = AvroSchema.create(it)
                schema.fullName to schema
            } catch (e: Exception) {
                logger.warn("Cannot parse JSON in schema $it")
                null
            }
        }.filterNotNull().toMap()

        // NOTE: there's probably a much more efficient way of doing this.
        val ordered = mutableListOf<String>()
        // take those with no dependencies first
        var remaining = all.values.sortedBy { it.needs.count() }.map { it.fullName }
        while (remaining.isNotEmpty()) {
            val newRemaining = mutableListOf<String>()
            remaining
                .filter { !ordered.contains(it) }
                .map {
                    val schema = all[it]!!
                    if (schema.needs.isEmpty() || ordered.containsAll(schema.needs)) {
                        ordered.add(it)
                    } else {
                        newRemaining.add(it)
                    }
                }

            if (remaining.size == newRemaining.size) {
                logger.warn(
                    "Cannot parse the following schemas due to missing dependencies:\n " +
                        "${remaining.map {
                            "   $it: ${all[it]!!.needs}"
                        }
                        }\n" +
                        "Has: $ordered"
                )
                break
            }
            remaining = newRemaining
        }

        val schemaParser = Schema.Parser()
        val avroSchemas = ordered.map {
            try {
                schemaParser.parse(all[it]!!.schema)
            } catch (e: Exception) {
                logger.warn("Could not parse schema $it: ${e.message}")
                null
            }
        }.filterNotNull()
        val schemaRegistry = schemaRegistryFactory.create(avroSchemas)
        schemaRegistry.failedToParse.addAll(remaining.map { all[it] }.filterNotNull())

        return schemaRegistry
    }
}
