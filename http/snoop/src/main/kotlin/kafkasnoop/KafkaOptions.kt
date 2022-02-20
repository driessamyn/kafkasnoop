package kafkasnoop

import java.util.Properties

interface KafkaOptions {
    val brokerAddress: String
    val kafkaCliOptions: Map<String, String>
    val clientProperties: Properties
}
