package kafkasnoop.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Partition
import kafkasnoop.dto.Topic
import org.apache.kafka.common.TopicPartition

fun Route.topics(kafkaClientFactory: KafkaClientFactory) {
    get("/api") {
        call.run {
            respond(
                kafkaClientFactory
                    .createConsumer().use {
                        it.listTopics()
                            .map { t ->
                                val partitions = t.value.map { p -> TopicPartition(t.key, p.partition()) }
                                val beggingOffsets = it.beginningOffsets(partitions)
                                    .map { o -> o.key.partition() to o.value }.toMap()
                                val endOffsets = it.endOffsets(partitions)
                                    .map { o -> o.key.partition() to o.value }.toMap()

                                Topic(
                                    t.key,
                                    t.value.map { p ->
                                        Partition(
                                            p.partition(),
                                            beggingOffsets.getOrDefault(p.partition(), 0L),
                                            endOffsets.getOrDefault(p.partition(), 0L),
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
