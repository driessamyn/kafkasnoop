package kafkasnoop.serialisation.avro.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.serialisation.avro.MessageSchemaOptions

fun NormalOpenAPIRoute.serialiserApiRoutes(
    schemaRegistry: SchemaRegistry,
    messageEnvelopeOptions: MessageSchemaOptions
) {
    route("/json2").deserialise(schemaRegistry, messageEnvelopeOptions)
}
