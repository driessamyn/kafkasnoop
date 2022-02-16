package kafkasnoop

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.http.InstantJsonSerialiser
import kafkasnoop.routes.messages
import kafkasnoop.routes.messagesWs
import kafkasnoop.routes.openApi
import kafkasnoop.routes.topics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import java.time.Instant

class Server(private val kafkaClientFactory: KafkaClientFactory) {
    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }

    @ExperimentalCoroutinesApi
    fun start(port: Int) {
        logger.info("Starting HTTP server")
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                    disableHtmlEscaping()
                    registerTypeAdapter(
                        Instant::class.java,
                        InstantJsonSerialiser()
                    )
                }
            }
            install(WebSockets)
            install(OpenAPIGen) {
                // basic info
                info {
                    version = "0.0.3"
                    title = "KafkaSnoop API"
                    description = "HTTP API for Snooping on Kafka messages"
                }
            }

            routing {
                openApi()
                messagesWs(kafkaClientFactory)
            }
            apiRouting {
                route("/api").topics(kafkaClientFactory)
                route("/api/{topic}").messages(kafkaClientFactory)
            }
        }.start(wait = true)
    }
}
