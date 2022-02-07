package kafkasnoop.routes

import kafkasnoop.dto.Message
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.math.max

class MessageProcessor(
    private val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>,
    private val topicName: String,
    // todo: take offset from query string & filter partitions
    private val startOffset: Long? = null,
) {
    private val partitions: List<TopicPartition>
    init {
        logger.debug("Getting messages for $topicName")

        partitions = kafkaConsumer.partitionsFor(topicName).map {
            TopicPartition(it.topic(), it.partition())
        }
        kafkaConsumer.assign(partitions)
    }

    fun startProcess(maxMsgCount: Int = Int.MAX_VALUE) =
        sequence {
            val beggingOffsets = kafkaConsumer.beginningOffsets(partitions)
            val endOffsets = kafkaConsumer.endOffsets(partitions)

            if (logger.isDebugEnabled) {
                beggingOffsets.forEach {
                    logger.debug("Begin Offset for ${it.key} is ${it.value}")
                }
                endOffsets.forEach {
                    logger.debug("End Offset for ${it.key} is ${it.value}")
                }
            }

            // default to rewinding to 100 or max msg count
            val offsetDiff = if (maxMsgCount == Int.MAX_VALUE) 100 else maxMsgCount
            val startOffset = max(endOffsets.maxOf { it.value }.minus(offsetDiff), 0)

            logger.info("Return messasges from offset $startOffset")

            partitions.forEach { p ->
                logger.debug("Min offset for partition $p is ${beggingOffsets[p]}")
                logger.debug("Max offset for partition $p is ${endOffsets[p]}")
                val offset = max(startOffset, beggingOffsets[p] ?: 0)
                logger.info("Start at offset for partition $p is $offset")
                kafkaConsumer.seek(p, offset)
            }

            var msgCount = 0
            while (msgCount < maxMsgCount) {
                val msgs = partitions.map { partition ->
                    kafkaConsumer
                        .poll(Duration.ofMillis(0)).records(partition)
                        .map { record ->
                            val key = String(record.key(), Charsets.UTF_8)
                            val value = String(record.value(), Charsets.UTF_8)
                            Message(record.offset(), partition.toString(), key, value)
                        }
                }.flatten()

                if (msgs.isEmpty())
                    Thread.sleep(1000)
                else {
                    logger.debug("Found ${msgs.count()} on $topicName: ${msgs.groupBy { it.partition }.map { it.key to it.value.maxOf { it.offset } }.toMap()}")
                    yieldAll(msgs.sortedBy { it.offset })
                }

                msgCount += msgs.count()
            }
            logger.debug("stopping to process")
        }
}
