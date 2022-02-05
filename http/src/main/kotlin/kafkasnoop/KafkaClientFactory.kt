package kafkasnoop

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties

class KafkaClientFactory(private val props: Properties) {
    fun createConsumer(): KafkaConsumer<ByteArray, ByteArray> {
        return KafkaConsumer<ByteArray, ByteArray>(props)
    }
}
