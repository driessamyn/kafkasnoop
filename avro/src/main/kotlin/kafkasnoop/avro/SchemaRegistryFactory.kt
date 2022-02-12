package kafkasnoop.avro

import org.apache.avro.Schema

class SchemaRegistryFactory {
    fun create(schemas: List<Schema>): SchemaRegistry {
        return SchemaRegistry(schemas)
    }
}
