package kafkasnoop.avro

import com.google.gson.JsonParser

data class AvroSchema(val fullName: String, val needs: List<String>, val schema: String) {
    companion object {
        private val primitive = listOf(
            "null",
            "int",
            "long",
            "float",
            "double",
            "bytes",
            "string",
            "boolean",
        )
        fun create(schema: String): AvroSchema {
            val json = JsonParser.parseString(schema).asJsonObject
            val ns = json.get("namespace").asString
            val name = json.get("name").asString
            val needs = if (json.has("fields")) {
                json.get("fields").asJsonArray
                    .asSequence()
                    .filter { it.isJsonObject }
                    .map { it.asJsonObject.get("type") }
                    .filter { it.isJsonPrimitive }
                    .map {
                        it.asString
                    }
                    .filter { !primitive.contains(it) }
                    .map {
                        // if the type doesn't contain . we assume the same namespace as the parent type
                        // TODO: not sure if this is according to the AVRO spec
                        if (it.contains('.'))
                            it
                        else
                            "$ns.$it"
                    }
                    .toList()
            } else {
                emptyList()
            }
            return AvroSchema("$ns.$name", needs, schema)
        }
    }
}
