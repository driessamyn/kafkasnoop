package kafkasnoop.test

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.slf4j.LoggerFactory

class PublishCommand : CliktCommand(help = "Publish test data") {
    companion object {
        private val logger = LoggerFactory.getLogger(PublishCommand::class.java)
        const val HERO_TOPIC = "super-heros"
    }

    private val brokerAddress: String by option("-b", "--broker", help = "Kafka broker address")
        .default("localhost:9092")

    override fun run() {
        val producerProperties =
            mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokerAddress,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,

            ).toProperties()
        val adminProperties = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to brokerAddress,
        ).toProperties()

        // TODO: wait properly
        logger.info("Create Topic $HERO_TOPIC.")
        Admin.create(adminProperties).use { admin ->
            val topic = NewTopic(HERO_TOPIC, 4, 1)
            val topics = admin.listTopics().names().get()
            if (topics.contains(topic.name())) {
                logger.info("Delete topic ${topic.name()}")
                admin.deleteTopics(listOf(topic).map { it.name() }).all().get()

                // wait for it to be gone
                // TODO: do better
                Thread.sleep(1000)
            }

            admin.createTopics(listOf(topic)).all().get()
        }

        logger.info("Publish Examples.")
        KafkaProducer<ByteArray, ByteArray>(producerProperties).use {
            ExamplesWithEnvelopeAndFingerPrint.produce(it)
        }
    }
}
