package kafkasnoop.serialisation.avro.dto

import com.papsign.ktor.openapigen.annotations.Response

@Response("AVRO Schema Registry")
data class SchemaRegistry(
    val schemas: List<Schema>,
    val failedToParse: List<FailedToParseSchema>
)

data class Schema(
    val name: String,
    val sha256FingerPrint: String,
    val aliases: List<String>,
    val type: String,
    val fields: List<String>,
    val contentUrl: String,
)

data class FailedToParseSchema(val name: String, val dependencies: List<String>)
