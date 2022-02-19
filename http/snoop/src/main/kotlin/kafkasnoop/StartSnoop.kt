package kafkasnoop

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.http.*
import kafkasnoop.http.ServerOptionsImpl
import kafkasnoop.serialisation.ByteToStringService
import kafkasnoop.serialisation.SimpleByteToStringDeserialiser
import org.slf4j.LoggerFactory

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StartSnoop : CliktCommand() {
    companion object {
        private val logger = LoggerFactory.getLogger(StartSnoop::class.java)
    }

    private val serverOptions by ServerOptionsImpl()
    private val kafkaOptions by KafkaOptionsImpl()

    private val serialiserService: String? by option(
        "-s",
        "--serialiser",
        "--serializer",
        help = "Endpoint for the Serialisation Service. " +
            "If not set, will not deserialise keys and values"
    )

    override fun run() {
        logger.info("Starting with Kafka properties: ${kafkaOptions.clientProperties}")

        val deserialiser = if (serialiserService.isNullOrBlank()) {
            SimpleByteToStringDeserialiser()
        } else {
            ByteToStringService(Url(serialiserService!!))
        }

        KafkaClientFactory(kafkaOptions.clientProperties).let {
            Server(it).start(serverOptions.port, deserialiser)
        }
    }
}
