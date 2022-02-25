package kafkasnoop.serialisation.avro

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kafkasnoop.avro.SchemaLoader
import kafkasnoop.avro.SchemaRegistry
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

class MessageSchemaOptionsImpl :
    MessageSchemaOptions,
    OptionGroup("Message Schema Options") {
    companion object {
        private val logger = LoggerFactory.getLogger(MessageSchemaOptions::class.java)
    }

    override val schemaPath: String? by option(
        "-s", "--schema-path",
        help = "Path to directory containing AVRO schema files."
    )
    override val envelopeSchemaName: String? by option(
        "-e", "--envelope",
        help = "Name of the schema used as a message envelope. " +
            "If left blank, will not use Message Envelope."
    )
    override val schemaNameFieldInEnvelope: String? by option(
        "--envelope-schema-name-field",
        help = "Field used to look up Schema Name in Message Envelope. " +
            "Will be ignored unless -e option is set.",
    )
    override val payloadFieldInEnvelope: String? by option(
        "--envelope-payload-field",
        help = "Field used to look up Payload in Message Envelope. " +
            "Will be ignored unless -e option is set."
    )
    override val schemaFingerPrintFieldInEnvelope: String? by option(
        "--envelope-schema-fingerprint-field",
        help = "Field used to look up Schema Fingerprint in Message Envelope. " +
            "Will be ignored unless -e option is set. " +
            "Will also be ignore if --envelope-schema-name-field is set."
    )
    override val schemaFingerPrintAlgorithmFieldInEnvelope: String? by option(
        "--envelope-schema-fingerprint-algorithm-field",
        help = "Field used to look up Schema Fingerprint Algorithm in Message Envelope. " +
            "Will be ignored unless -e option is set. " +
            "Will be ignored unless -e and --envelope-schema-fingerprint-field options" +
            "are set. If not specified will use value passed in using " +
            "--envelope-schema-fingerprint-algorithm option or default (SHA-256)"
    )
    override val schemaFingerPrintAlgorithm: String by option(
        "--envelope-schema-fingerprint-algorithm",
        help = "Algorithm used to calculate schema fingerprint " +
            "Will be ignored unless -e and --envelope-schema-fingerprint-field options" +
            "are set. Will also be ignore if --envelope-schema-fingerprint-algorithm-field is used. " +
            "Default is SHA-256"
    ).default("SHA-256")

    val schemaRegistry by lazy {
        if (schemaPath.isNullOrBlank()) {
            PrintMessage("You must specify schema-path", true)
            throw ProgramResult(1)
        }

        val schemaPath = Path(schemaPath!!)
        if (!schemaPath.isDirectory()) {
            PrintMessage("$schemaPath is not a valid directory", true)
            throw ProgramResult(1)
        }

        logger.info("Initialising schemaRegistry with: $schemaPath")

        SchemaLoader().createFromDir(schemaPath)
    }
}
