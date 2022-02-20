package kafkasnoop.serialisation.avro

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kafkasnoop.http.ServerOptionsImpl
import org.slf4j.LoggerFactory

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StartService : CliktCommand() {
    companion object {
        private val logger = LoggerFactory.getLogger(StartService::class.java)
    }

    private val serverOptions by ServerOptionsImpl()
    private val messageSchemaOptions by MessageSchemaOptionsImpl()

    override fun run() {
        logger.info("Starting Server with $serverOptions, $messageSchemaOptions")
        Server(messageSchemaOptions.schemaRegistry, messageSchemaOptions)
            .start(serverOptions.port)
    }
}
