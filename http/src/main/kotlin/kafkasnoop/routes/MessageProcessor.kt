package kafkasnoop.routes

import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import org.apache.kafka.common.TopicPartition
import java.time.Duration

class MessageProcessor(
    private val kafkaClientFactory: KafkaClientFactory,
    private val topicName: String,
    // todo: take offset from query string & filter partitions
    private val startOffset: Long = 0L,
) {
    val partitions: List<TopicPartition>
    init {
        logger.debug("Getting messages for $topicName")

        kafkaClientFactory.getOrCreateConsumer().let {
            partitions = it.partitionsFor(topicName).map {
                TopicPartition(it.topic(), it.partition())
            }
            it.assign(partitions)
            val beggingOffsets = it.beginningOffsets(partitions)

            partitions.forEach { p ->
                val pOffset = java.lang.Long.max(beggingOffsets[p] ?: 0, startOffset)
                logger.info("Begging offset for partition $p is $pOffset")
                it.seek(p, pOffset)
            }
        }
    }

    fun getMessages(partition: TopicPartition): List<Message> {
        val polled = kafkaClientFactory.getOrCreateConsumer()
            .poll(Duration.ofMillis(100)).records(partition)

        return polled.map {
            // TODO: deserialising
            val key = String(it.key(), Charsets.UTF_8)
            val value = String(it.value(), Charsets.UTF_8)
            Message(key, value)
        }
    }
}
