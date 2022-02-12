package kafkasnoop

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID

class KafkaClientFactory(private val props: Properties) {
    companion object {
        private val logger = LoggerFactory.getLogger(KafkaClientFactory::class.java)
    }

    fun createConsumer(): KafkaConsumer<ByteArray, ByteArray> {
        val consumerId = "kafkasnoop-consumer-${UUID.randomUUID()}"
        logger.info("Create consumer $consumerId")
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, consumerId)
        return KafkaConsumer<ByteArray, ByteArray>(props)
    }
}
