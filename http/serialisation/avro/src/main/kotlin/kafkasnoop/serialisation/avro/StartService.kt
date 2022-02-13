package kafkasnoop.serialisation.avro

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kafkasnoop.avro.SchemaLoader
import kafkasnoop.avro.SchemaRegistry
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

class StartService : CliktCommand() {
    companion object {
        private val logger = LoggerFactory.getLogger(StartService::class.java)
    }

    private val port: Int by option(
        "-p", "--port",
        help = "Port to expose KafkaSnoop Serialisation Service on"
    ).int()
        .default(9000)
    private val schemaPath: String? by option(
        "-s", "--schema-path",
        help = "Path to directory containing AVRO schema files"
    )

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    override fun run() {
        if (null == schemaPath) {
            PrintMessage("You must specify schema-path", true)
            throw ProgramResult(1)
        }

        val schemaPath = Path(schemaPath!!)
        if (!schemaPath.isDirectory()) {
            PrintMessage("$schemaPath is not a valid directory", true)
            throw ProgramResult(1)
        }

        logger.info("Starting Serialisation service with $schemaPath")

        val schemaLoader = SchemaLoader().createFromDir(schemaPath)
        val schemaRegistry = SchemaRegistry(schemaLoader.all)

        Server(schemaRegistry).start(port)
    }
}
