package kafkasnoop

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties

class KafkaClientFactory(props: Properties) : AutoCloseable {
    private val consumer by lazy {
        KafkaConsumer<ByteArray, ByteArray>(props)
    }

    fun getOrCreateConsumer(): KafkaConsumer<ByteArray, ByteArray> = consumer
    override fun close() {
        consumer.close()
    }
}
