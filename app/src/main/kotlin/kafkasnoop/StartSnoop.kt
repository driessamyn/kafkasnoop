package kafkasnoop

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory

class StartSnoop : CliktCommand() {
    companion object {
        private val logger = LoggerFactory.getLogger(StartSnoop::class.java)
    }

    private val brokerAddress: String by option("-b", "--broker", help = "Kafka broker address")
        .default("localhost:9092")
    private val kafkaCliOptions: Map<String, String> by
    option("-k", "--kafka-prop", help = "Optional Kafka client properties")
        .associate()

    override fun run() {
        val kafkaClientOptions = (
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokerAddress,
                ConsumerConfig.GROUP_ID_CONFIG to "kafkasnoop",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "100",
                ConsumerConfig.CLIENT_ID_CONFIG to "kafkasnoop-consumer",
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed"
            ) + kafkaCliOptions
            ).toProperties()

        logger.info("Starting with $kafkaClientOptions")

        val kc = KafkaClientFactory(kafkaClientOptions)
        Server(kc).start()
    }
}
