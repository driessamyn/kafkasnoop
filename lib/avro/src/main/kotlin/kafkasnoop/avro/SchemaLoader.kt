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
        val all = schemas.mapNotNull {
            try {
                AvroSchema.create(it)
            } catch (e: Exception) {
                logger.warn("Cannot parse JSON in schema $it")
                null
            }
        }

        val schemasSortedByDependency = sortByDependencies(all)

        val schemaParser = Schema.Parser()

        val failedToParse = mutableListOf<AvroSchema>()
        val avroSchemas = schemasSortedByDependency.map {
            try {
                schemaParser.parse(it.schema)
            } catch (e: Exception) {
                logger.warn("Could not parse schema $it: ${e.message}")
                failedToParse.add(it)
                null
            }
        }.filterNotNull()
        val schemaRegistry = schemaRegistryFactory.create(avroSchemas)
        schemaRegistry.failedToParse.addAll(failedToParse)

        return schemaRegistry
    }

    private fun sortByDependencies(schemas: List<AvroSchema>): List<AvroSchema> {
        val dependencies = schemas.associateBy({ it.fullName }, { it.needs.toMutableSet() })
        val ready = dependencies.keys.filter { dependencies[it]!!.isEmpty() }.toMutableSet()
        val sortedSchemas = mutableListOf<AvroSchema>()
        while (ready.isNotEmpty()) {
            val schemaName = ready.first()
            sortedSchemas.add(schemas.first { it.fullName == schemaName })
            ready.remove(schemaName)
            // Search what other schemas depend on the current one
            dependencies.forEach { (name, needs) ->
                if (schemaName in needs) {
                    needs.remove(schemaName)
                    if (needs.isEmpty()) {
                        ready.add(name)
                    }
                }
            }
        }
        // If any schemas were not matched add them last
        // There are schemas which define, e.g. enum avro type within their definition.
        // Their dependency will be resolved by the avro parser
        val unresolved = schemas.filterNot { it in sortedSchemas }
        return sortedSchemas + unresolved
    }
}
