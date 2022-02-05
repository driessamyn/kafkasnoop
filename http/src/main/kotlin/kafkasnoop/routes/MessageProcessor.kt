package kafkasnoop.routes

import kafkasnoop.dto.Message
import kotlinx.coroutines.yield
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration

class MessageProcessor(
    private val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>,
    private val topicName: String,
    // todo: take offset from query string & filter partitions
    private val startOffset: Long = 0L,
) {
    private val partitions: List<TopicPartition>
    init {
        logger.debug("Getting messages for $topicName")

        partitions = kafkaConsumer.partitionsFor(topicName).map {
            TopicPartition(it.topic(), it.partition())
        }
        kafkaConsumer.assign(partitions)
        val beggingOffsets = kafkaConsumer.beginningOffsets(partitions)

        partitions.forEach { p ->
            val pOffset = java.lang.Long.max(beggingOffsets[p] ?: 0, startOffset)
            logger.info("Begging offset for partition $p is $pOffset")
            kafkaConsumer.seek(p, pOffset)
        }
    }

    fun startProcess(maxMsgCount: Int = Int.MAX_VALUE) =
        sequence {
            var msgCount = 0
            while (msgCount < maxMsgCount) {
                partitions.forEach { partition ->
                    kafkaConsumer
                        .poll(Duration.ofMillis(100)).records(partition)
                        .forEach { record ->
                            val key = String(record.key(), Charsets.UTF_8)
                            val value = String(record.value(), Charsets.UTF_8)
                            val msg = Message(partition.toString(), key, value)
                            yield(msg)
                            msgCount += 1
                        }
                }
            }
            logger.debug("stopping to process")
        }
}
