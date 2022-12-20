package kafkasnoop.avro

import com.google.gson.JsonElement
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
                    .flatMap {
                        extractNonPrimitiveTypes(it)
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

        private fun extractNonPrimitiveTypes(elem: JsonElement?): List<String> {

            if (elem == null) {
                return emptyList()
            }

            return if (elem.isJsonObject) {
                extractNonPrimitiveTypes(elem.asJsonObject.get("type"))
                extractNonPrimitiveTypes(elem.asJsonObject.get("items"))
            } else if (elem.isJsonArray) {
                elem.asJsonArray.filter {
                    if (it.isJsonPrimitive) {
                        !primitive.contains(it.asString)
                    } else {
                        false
                    }
                }.map { it.asString }
            } else {
                if (elem.isJsonPrimitive && !primitive.contains(elem.asString)) {
                    listOf(elem.asString)
                } else {
                    emptyList()
                }
            }
        }
    }
}
