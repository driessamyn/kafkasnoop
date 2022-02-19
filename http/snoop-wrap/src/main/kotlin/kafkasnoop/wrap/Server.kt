package kafkasnoop.wrap

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory
import kafkasnoop.Server
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.http.installContentNegotiation
import kafkasnoop.http.openApi
import kafkasnoop.routes.messagesWs
import kafkasnoop.routes.snoopApiRoutes
import kafkasnoop.serialisation.ByteToStringService
import kafkasnoop.serialisation.MessageDeserialiser
import kafkasnoop.serialisation.avro.MessageSchemaOptions
import kafkasnoop.serialisation.avro.routes.serialiserRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory

@ExperimentalCoroutinesApi
class Server(
    private val schemaRegistry: SchemaRegistry,
    private val messageEnvelopeOptions: MessageSchemaOptions,
    private val kafkaClientFactory: KafkaClientFactory,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }

    fun start(port: Int) {
        logger.info("Starting HTTP server")
        embeddedServer(Netty, port = port) {
            // serialiaser is local
            val deserialiser =
                ByteToStringService(Url("http://localhost:$port/json"))

            wrappedServer(
                schemaRegistry,
                messageEnvelopeOptions,
                kafkaClientFactory,
                deserialiser,
            )
        }.start(wait = true)
    }

    fun Application.wrappedServer(
        schemaRegistry: SchemaRegistry,
        messageEnvelopeOptions: MessageSchemaOptions,
        kafkaClientFactory: KafkaClientFactory,
        deserialiser: MessageDeserialiser,
    ) {
        installContentNegotiation()
        install(WebSockets)
        install(OpenAPIGen) {
            // basic info
            info {
                version = "0.0.3"
                title = "KafkaSnoop & AVRO Serialisation API"
                description = "HTTP API for both KafkaSnoop and AVRO Serialisation"
            }
        }

        routing {
            openApi()
            messagesWs(kafkaClientFactory, deserialiser)
            serialiserRoutes(schemaRegistry, messageEnvelopeOptions)
        }
        apiRouting {
            snoopApiRoutes(kafkaClientFactory, deserialiser)
        }
    }
}
