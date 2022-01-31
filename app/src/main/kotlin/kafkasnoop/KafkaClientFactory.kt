package kafkasnoop

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties

class KafkaClientFactory(props: Properties) {
    private val consumer by lazy {
        KafkaConsumer<Any, Any>(props)
    }

    fun getOrCreatConsumer(): KafkaConsumer<Any, Any> = consumer
}
