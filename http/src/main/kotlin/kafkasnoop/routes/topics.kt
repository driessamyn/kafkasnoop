package kafkasnoop.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Partition
import kafkasnoop.dto.Topic

fun Route.topics(kafkaClientFactory: KafkaClientFactory) {
    get("/api") {
        call.run {
            respond(
                kafkaClientFactory
                    .createConsumer().use {
                        it.listTopics()
                            .map { t ->
                                Topic(
                                    t.key,
                                    t.value.map { p ->
                                        Partition(
                                            p.partition(),
                                            p.inSyncReplicas().count(),
                                            p.offlineReplicas().count()
                                        )
                                    }
                                )
                            }
                    }
            )
        }
    }
}
