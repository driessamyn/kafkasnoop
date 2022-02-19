package kafkasnoop.serialisation.avro

import com.papsign.ktor.openapigen.OpenAPIGen
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.http.installContentNegotiation
import kafkasnoop.serialisation.avro.routes.serialiserRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory

class Server(
    private val schemaRegistry: SchemaRegistry,
    private val messageEnvelopeOptions: MessageSchemaOptions
) {

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }

    @ExperimentalCoroutinesApi
    fun start(port: Int) {
        logger.info("Starting HTTP server")
        embeddedServer(Netty, port = port) {
            serialisationServer(schemaRegistry, messageEnvelopeOptions)
        }.start(wait = true)
    }
}

fun Application.serialisationServer(
    schemaRegistry: SchemaRegistry,
    messageEnvelopeOptions: MessageSchemaOptions
) {
    installContentNegotiation()
    install(WebSockets)
    install(OpenAPIGen) {
        // basic info
        info {
            version = "0.0.3"
            title = "KafkaSnoop AVRO Serialisation API"
            description = "HTTP API for AVRO Serialisation"
        }
    }

    routing {
        serialiserRoutes(schemaRegistry, messageEnvelopeOptions)
    }
}
