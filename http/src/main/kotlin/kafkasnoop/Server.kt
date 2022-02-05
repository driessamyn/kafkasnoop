package kafkasnoop

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.routes.messages
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

            routing {
                topics(kafkaClientFactory)
                messages(kafkaClientFactory)
                webSocket {
                }
            }
        }.start(wait = true)
    }
}
