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
        )
        fun create(schema: String): AvroSchema {
            val json = JsonParser.parseString(schema).asJsonObject
            val ns = json.get("namespace").asString
            val name = json.get("name").asString
            val needs = json.get("fields").asJsonArray
                .filter { it.isJsonObject }
                .map { it.asJsonObject.get("type") }
                .filter { it.isJsonPrimitive }
                .map {
                    it.asString
                }
                .filter { !primitive.contains(it) }
            return AvroSchema("$ns.$name", needs, schema)
        }
    }
}
