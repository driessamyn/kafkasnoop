package kafkasnoop.wrap

import kafkasnoop.KafkaOptionsImpl
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kafkasnoop.KafkaClientFactory
import kafkasnoop.http.ServerOptionsImpl
import kafkasnoop.serialisation.avro.MessageSchemaOptionsImpl
import kafkasnoop.serialisation.avro.StartService
import org.slf4j.LoggerFactory

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StartServer : CliktCommand() {
    companion object {
        private val logger = LoggerFactory.getLogger(StartService::class.java)
    }

    private val serverOptions by ServerOptionsImpl()
    private val messageSchemaOptions by MessageSchemaOptionsImpl()
    private val kafkaOptions by KafkaOptionsImpl()

    override fun run() {
        logger.info("Starting Server with $serverOptions, $messageSchemaOptions, $kafkaOptions")
        KafkaClientFactory(kafkaOptions.clientProperties).let {
            Server(
                messageSchemaOptions.schemaRegistry,
                messageSchemaOptions,
                it
            ).start(serverOptions.port)
        }
    }
}
