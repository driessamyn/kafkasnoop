package kafkasnoop.serialisation.avro.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import kafkasnoop.avro.SchemaRegistry

fun NormalOpenAPIRoute.serialiserApiRoutes(
    schemaRegistry: SchemaRegistry,
) {
    route("/api/schemas").schemas(schemaRegistry)
    route("/api/schemas/{name}").schemaByName(schemaRegistry)
}
