package kafkasnoop.serialisation.avro

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.http.InstantJsonSerialiser
import kafkasnoop.serialisation.avro.routes.openApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import java.time.Instant

class Server(schemaRegistry: SchemaRegistry) {
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
                    title = "KafkaSnoop AVRO Serialisation API"
                    description = "HTTP API for AVRO Serialisation"
                }
            }

            routing {
                openApi()
            }
            apiRouting {
            }
        }.start(wait = true)
    }
}
