package kafkasnoop.serialisation.avro.routes

import com.papsign.ktor.openapigen.openAPIGen
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.openApi() {
    get("/api/openapi.json") {
        call.respond(application.openAPIGen.api.serialize())
    }
    get("/") {
        call.respondRedirect("/swagger-ui/index.html?url=/api/openapi.json", true)
    }
}
