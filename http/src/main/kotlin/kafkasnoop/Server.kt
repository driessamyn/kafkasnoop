package kafkasnoop

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
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
import kafkasnoop.routes.messages
import kafkasnoop.routes.messagesWs
import kafkasnoop.routes.openApi
import kafkasnoop.routes.topics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Server(private val kafkaClientFactory: KafkaClientFactory) {
    companion object {
        private val logger = LoggerFactory.getLogger(StartSnoop::class.java)
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
                        object : JsonSerializer<Instant> {
                            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))

                            override fun serialize(instant: Instant, typeOfSrc: Type, context: JsonSerializationContext):
                                JsonElement {
                                return JsonPrimitive(formatter.format(instant))
                            }
                        }
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
