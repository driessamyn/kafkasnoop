package kafkasnoop

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.http.installContentNegotiation
import kafkasnoop.http.openApi
import kafkasnoop.serialisation.MessageDeserialiser
import kafkasnoop.routes.messagesWs
import kafkasnoop.routes.snoopApiRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory

class Server(private val kafkaClientFactory: KafkaClientFactory) {
    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }

    @ExperimentalCoroutinesApi
    fun start(port: Int, messageDeserialiser: MessageDeserialiser) {
        logger.info("Starting HTTP server")
        embeddedServer(Netty, port = port) {
            installContentNegotiation()
            install(WebSockets)
            install(OpenAPIGen) {
                info {
                    version = "0.0.3"
                    title = "KafkaSnoop API"
                    description = "HTTP API for Snooping on Kafka messages"
                }
            }

            routing {
                openApi()
                messagesWs(kafkaClientFactory, messageDeserialiser)
            }
            apiRouting {
                snoopApiRoutes(kafkaClientFactory, messageDeserialiser)
            }
        }.start(wait = true)
    }
}
