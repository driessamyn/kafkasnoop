package kafkasnoop.routes

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Partition
import kafkasnoop.dto.Topic
import org.apache.kafka.common.TopicPartition

fun NormalOpenAPIRoute.topics(kafkaClientFactory: KafkaClientFactory) {
    get<Unit, List<Topic>>(
        info("Topics", "Get Topics and Partition Details"),
        example = listOf(
            Topic(
                "topic",
                listOf(
                    Partition(
                        0,
                        0,
                        123,
                        2,
                        1
                    )
                )
            )
        )
    ) {
        kafkaClientFactory
            .createConsumer().use {
                respond(
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
                )
            }
    }
}
