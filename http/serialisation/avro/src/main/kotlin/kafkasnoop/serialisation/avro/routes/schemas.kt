package kafkasnoop.serialisation.avro.routes

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.features.*
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.serialisation.avro.dto.FailedToParseSchema
import kafkasnoop.serialisation.avro.dto.Schema
import java.net.URLEncoder

fun NormalOpenAPIRoute.schemas(
    schemaRegistry: SchemaRegistry
) {
    get<Unit, kafkasnoop.serialisation.avro.dto.SchemaRegistry>(
        info("Schema Registry", "Get details of all schemas in the registry")
    ) {
        respond(
            kafkasnoop.serialisation.avro.dto.SchemaRegistry(
                schemaRegistry.all.map {
                    val fields = if (it.type == org.apache.avro.Schema.Type.ENUM) {
                        emptyList()
                    } else {
                        it.fields.map { f -> "${f.name() ?: ""}[${f.schema().fullName}]" }
                    }
                    Schema(
                        it.fullName,
                        schemaRegistry.getBase64FingerPrintFor(it.fullName) ?: "",
                        it.aliases.toList(),
                        it.type.name,
                        fields,
                        "/api/schemas/${URLEncoder.encode(it.fullName, Charsets.UTF_8)}",
                    )
                },
                schemaRegistry.failedToParse.map { s -> FailedToParseSchema(s.fullName, s.needs) }
            )
        )
    }
}

data class SchemaNameParam(@PathParam("Schema Full Name") val name: String)

fun NormalOpenAPIRoute.schemaByName(schemaRegistry: SchemaRegistry) {
    get<SchemaNameParam, JsonElement>(
        info("Schema Content", "Get details of all schemas in the registry")
    ) { param ->
        val schema = schemaRegistry.getByName(param.name)
            ?: throw NotFoundException("Schema with name ${param.name} not found")
        respond(
            JsonParser.parseString(schema.toString(true))
        )
    }
}
