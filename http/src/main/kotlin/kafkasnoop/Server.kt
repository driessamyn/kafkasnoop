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
import kafkasnoop.routes.messageOpenApi
import kafkasnoop.routes.messagesWs
import kafkasnoop.routes.openApi
import kafkasnoop.routes.topics
import org.slf4j.LoggerFactory

class Server(private val kafkaClientFactory: KafkaClientFactory) {
    companion object {
        private val logger = LoggerFactory.getLogger(StartSnoop::class.java)
    }

    fun start(port: Int) {
        logger.info("Starting HTTP server")
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                    disableHtmlEscaping()
                }
            }
            install(WebSockets)
            install(OpenAPIGen) {
                // basic info
                info {
                    version = "0.0.1"
                    title = "KafkaSnoop API"
                    description = "HTTP API for Snooping on Kafka messages"
                }
            }

            routing {
                openApi()

                topics(kafkaClientFactory)
                messagesWs(kafkaClientFactory)
            }
            apiRouting {
                route("/api/{topic}").messageOpenApi(kafkaClientFactory)
            }
        }.start(wait = true)
    }


}

