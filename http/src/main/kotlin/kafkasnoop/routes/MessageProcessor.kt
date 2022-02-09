package kafkasnoop.routes

import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import kotlin.math.max

/**
 * Message processor
 *
 * NOTE: changed this to use 1 consumer per partition.
 * According to the docs, reading assigning multiple partitions to a consumer without a cosnumer group should
 * be fine, but it appeared unreliable. This probably means something was wrong in my code, but will come back to this.
 */
class MessageProcessor(
    private val kafkaClientFactory: KafkaClientFactory,
    private val topicName: String,
    // todo: take offset from query string & filter partitions
    private val startOffset: Long? = null,
) {
    val partitions: List<TopicPartition>
    init {
        logger.debug("Getting messages for $topicName")
        kafkaClientFactory.createConsumer().use {
            partitions = it.partitionsFor(topicName).map {
                TopicPartition(it.topic(), it.partition())
            }
        }
    }

    fun startProcess(partition: TopicPartition, maxMsgCount: Int = Int.MAX_VALUE) =
        sequence {
            kafkaClientFactory.createConsumer().use { kafkaConsumer ->
                kafkaConsumer.assign(listOf(partition))
                val beggingOffsets = kafkaConsumer.beginningOffsets(partitions)
                val endOffsets = kafkaConsumer.endOffsets(partitions)

                // default to rewinding to 5 or max msg count
                val offsetDiff = if (maxMsgCount == Int.MAX_VALUE) 5 else maxMsgCount
                logger.info("Return messages from offset $startOffset")
                logger.debug("Min offset for partition $partition is ${beggingOffsets[partition]}")
                logger.debug("Max offset for partition $partition is ${endOffsets[partition]}")
                val startOffset = max(endOffsets[partition]?.minus(offsetDiff) ?: 0L, 0L)
                val offset = max(startOffset, beggingOffsets[partition] ?: 0)
                val messageCount = endOffsets.getOrDefault(partition, 0) - offset
                logger.info("Loading $messageCount from $partition starting at $offset")
                kafkaConsumer.seek(partition, offset)

                var messagesLoaded = 0
                var emptyPolls = 0
                // TODO: work out why we sometimes never get all the messages.
                while ((maxMsgCount == Int.MAX_VALUE || emptyPolls <= 5) && messagesLoaded < messageCount) {
                    logger.debug("Polling $partition from ${kafkaConsumer.position(partition)}")
                    val msgs = kafkaConsumer
                        .poll(Duration.ofMillis(200)).records(partition)
                        .map { record ->
                            logger.debug("Found message $partition: ${record.offset()}")
                            val key = String(record.key(), Charsets.UTF_8)
                            val value = String(record.value(), Charsets.UTF_8)
                            Message(record.offset(), partition.toString(), key, value, record.timestamp())
                        }

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
                    logger.debug("Loaded $messagesLoaded out of $messageCount")
                }
                logger.debug("stopping to process")
            }
        }
}
