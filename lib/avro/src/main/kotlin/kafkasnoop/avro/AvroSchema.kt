package kafkasnoop.avro

import com.google.gson.JsonElement
import com.google.gson.JsonParser

data class AvroSchema(val fullName: String, val needs: List<String>, val schema: String) {
    companion object {
        private val primitiveTypes = listOf(
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
                        extractObjectDependencies(it)
                    }
                    .filter { !primitiveTypes.contains(it) }
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

        private fun extractObjectDependencies(elem: JsonElement?): List<String> {

            if (elem == null) {
                return emptyList()
            }

            return if (elem.isJsonObject) {
                when (elem.asJsonObject.get("type").asString) {
                    "enum" -> {
                        extractObjectDependencies(elem.asJsonObject.get("name"))
                    }
                    "array" -> {
                        extractObjectDependencies(elem.asJsonObject.get("items"))
                    }
                    "map" -> {
                        extractObjectDependencies(elem.asJsonObject.get("values"))
                    }
                    else -> emptyList()
                }
            } else if (elem.isJsonArray) {
                val objectsInArray = elem.asJsonArray.filter { it.isJsonObject }
                if (objectsInArray.isNotEmpty()) {
                    objectsInArray.flatMap { extractObjectDependencies(it.asJsonObject) }
                } else {
                    elem.asJsonArray
                        .filter { it.isJsonPrimitive && it.asString !in primitiveTypes }
                        .map { it.asString }
                }

            } else {
                if (!primitiveTypes.contains(elem.asString)) {
                    listOf(elem.asString)
                } else {
                    emptyList()
                }
            }
        }
    }
}
