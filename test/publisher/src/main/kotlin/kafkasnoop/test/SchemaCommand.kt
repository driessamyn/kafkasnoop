package kafkasnoop.test

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.avrokotlin.avro4k.Avro
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class SchemaCommand : CliktCommand(help = "Create Schema files") {
    companion object {
        private val logger = LoggerFactory.getLogger(SchemaCommand::class.java)
    }

    private val schemaDir: String by option("-d", "--dir", help = "Directory for schema files")
        .default("schemas")

    override fun run() {
        val dir = Path(schemaDir)
        Files.createDirectories(dir)
        logger.info("Create schemas in ${dir.toAbsolutePath()}")
        listOf(
            Car.serializer(),
            Superhero.serializer(),
            EnvelopeWithSchemaName.serializer(),
            EnvelopeWithFingerPrint.serializer(),
            EnvelopeWithFingerPrintAndAlgorithm.serializer(),
        ).forEach {
            File(dir.resolve("${it.descriptor.serialName}.avsc").toString())
                .writeText(Avro.default.schema(it).toString(true))
        }
    }
}
