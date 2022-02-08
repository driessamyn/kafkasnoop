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

            // default to rewinding to 5 or max msg count
            val offsetDiff = if (maxMsgCount == Int.MAX_VALUE) 5 else maxMsgCount
            logger.info("Return messasges from offset $startOffset")

            val messageCounts = partitions.map { p ->
                logger.debug("Min offset for partition $p is ${beggingOffsets[p]}")
                logger.debug("Max offset for partition $p is ${endOffsets[p]}")
                val startOffset = max(endOffsets[p]?.minus(offsetDiff) ?: 0L, 0L)
                val offset = max(startOffset, beggingOffsets[p] ?: 0)
                val numberOfMessages = endOffsets.getOrDefault(p, 0) - offset + 1
                logger.info("Loading $numberOfMessages from $p starting at $offset")
                kafkaConsumer.seek(p, offset)
                p to numberOfMessages
            }.toMap()

            val messagesToLoad = max(maxMsgCount, messageCounts.map { it.value.toInt() }.sum())
            var messagesLoaded = 0
            var emptyPolls = 0
            // TODO: work out why we sometimes never get all the messages.
            while ((maxMsgCount == Int.MAX_VALUE || emptyPolls <= 5) && messagesLoaded < messagesToLoad) {
                val msgs = partitions.map { partition ->
                    logger.debug("Polling $partition from ${kafkaConsumer.position(partition)}")
                    kafkaConsumer
                        .poll(Duration.ofMillis(200)).records(partition)
                        .map { record ->
                            logger.debug("Found message $partition: ${record.offset()}")
                            val key = String(record.key(), Charsets.UTF_8)
                            val value = String(record.value(), Charsets.UTF_8)
                            Message(record.offset(), partition.toString(), key, value, record.timestamp())
                        }
                }.flatten()

                if (msgs.isEmpty()) {
                    emptyPolls += 1
                    logger.debug("Empty polls: $emptyPolls")
                    Thread.sleep(200)
                } else {
                    logger.debug("Found ${msgs.count()} on $topicName: ${msgs.groupBy { it.partition }.map { it.key to it.value.maxOf { it.offset } }.toMap()}")
                    yieldAll(msgs.sortedBy { it.offset })
                    emptyPolls = 0
                }
                messagesLoaded += msgs.count()
                logger.debug("Loaded $messagesLoaded out of $messagesToLoad")
            }
            logger.debug("stopping to process")
        }
}
