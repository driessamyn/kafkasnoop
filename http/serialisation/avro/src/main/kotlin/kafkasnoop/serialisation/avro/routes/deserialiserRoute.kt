package kafkasnoop.serialisation.avro.routes

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.serialisation.avro.MessageSchemaOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Base64

fun Route.serialiserRoutes(
    schemaRegistry: SchemaRegistry,
    messageEnvelopeOptions: MessageSchemaOptions
) {
    val deserialiser = schemaRegistry.getDeserialiser()
    val envelopeDeserialiser = schemaRegistry.getEnvelopeDeserialser()

    post("/json") {
        val schemaParam = call.parameters["schema"]
        val schemaFingerPrintParam = call.parameters["schemaFingerPrint"]
        val schemaFingerPrintAlgorithmParam = call.parameters["schemaFingerPrintAlgorithm"]

        val bytes = withContext(Dispatchers.IO) {
            call.receive<InputStream>().use { s ->
                s.readAllBytes()
            }
        }

        // TODO: refactor/tidy this up, reduce duplication and complexity.
        if (null != messageEnvelopeOptions.envelopeSchemaName) {
            // use message envelope
            val envelopeSchema = messageEnvelopeOptions.envelopeSchemaName!!
            val payloadField = messageEnvelopeOptions.payloadFieldInEnvelope
                ?: throw BadRequestException("--envelope-payload-field must be set.")
            try {
                val response = responsePayload(
                    if (null != messageEnvelopeOptions.schemaNameFieldInEnvelope) {
                        envelopeDeserialiser.decode(
                            bytes,
                            envelopeSchema,
                            messageEnvelopeOptions.schemaNameFieldInEnvelope!!,
                            payloadField,
                        )
                    } else if (null != messageEnvelopeOptions.schemaFingerPrintFieldInEnvelope) {
                        envelopeDeserialiser.decodeFromFingerPrint(
                            bytes,
                            envelopeSchema,
                            messageEnvelopeOptions.schemaFingerPrintFieldInEnvelope!!,
                            payloadField,
                            messageEnvelopeOptions.schemaFingerPrintAlgorithmFieldInEnvelope,
                            messageEnvelopeOptions.schemaFingerPrintAlgorithm
                        )
                    } else {
                        throw BadRequestException("Must specify either schema name or fingerprint field.")
                    }
                )
                call.respond(response)
            }
            catch (e: Exception)
            {
                throw BadRequestException("Cannot deserialiase $bytes.")
            }

        } else if (null == schemaParam && null != schemaFingerPrintAlgorithmParam) {
            val fingerprintBin = Base64.getDecoder().decode(schemaFingerPrintParam)
            val fingerprintAlgo = schemaFingerPrintParam ?: SchemaRegistry.DEFAULT_FINGERPRINT_ALGORITHM
            call.respond(
                responsePayload(
                    deserialiser.decode(
                        bytes,
                        schemaRegistry.getByFingerPrint(fingerprintBin, fingerprintAlgo)?.fullName
                            ?: throw BadRequestException(
                                "Schema for fingerprint " +
                                    "[$fingerprintAlgo]$schemaFingerPrintParam cannot be found."
                            )
                    )
                )
            )
        } else if (null != schemaParam) {
            call.respond(
                responsePayload(deserialiser.decode(bytes, schemaParam))
            )
        } else {
            // fall back on trying all schemas
            call.respond(
                responsePayload(deserialiser.decode(bytes))
            )
        }
    }
}

private fun responsePayload(decoded: JsonObject): JsonObject {
    return responsePayload(JsonArray().apply { add(decoded) })
}

private fun responsePayload(decoded: JsonArray): JsonObject {
    return JsonObject().apply {
        add("schemas", decoded)
    }
}
