package kafkasnoop

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class Server(private val kafkaClientFactory: KafkaClientFactory) {
    fun start() {

        embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    // TODO: respond JSON
                    call.respondText(
                        kafkaClientFactory
                            .getOrCreatConsumer()
                            .listTopics()
                            .map { t ->
                                mapOf(
                                    "topic" to t.key,
                                    "partitions" to t.value.map { p ->
                                        mapOf(
                                            "partition" to p.partition(),
                                            "inSyncReplicas" to p.inSyncReplicas().count(),
                                            "offlineReplicas" to p.offlineReplicas().count(),
                                        ).toMap()
                                    }
                                ).toMap()
                            }.toString()
                    )
                }
            }
        }.start(wait = true)
    }
}
