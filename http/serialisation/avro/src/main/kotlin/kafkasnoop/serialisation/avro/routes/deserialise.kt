package kafkasnoop.serialisation.avro.routes

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.features.*
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.serialisation.avro.MessageSchemaOptions
import kafkasnoop.serialisation.avro.dto.RawAvro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64

// NOTE: this route doesn't work, I think due to a bug in the openAPI library.
// for now, use deserialiseRoute.kt

data class DeserialiseParams(
    @QueryParam(
        "Name of the schema to use to deserialise message. " +
            "When left blank, all schemas in the registry will be tried."
    )
    val schema: String?,
    @QueryParam(
        "BASE64 encoded Fingerprint for the schema to deserialise message. " +
            "This parameter will be ignored when name is also specified. " +
            "When Fingerprint is specified, fingerprint algorithm also needs to be specified (or default used)."
    )
    val schemaFingerPrint: String?,
    @QueryParam(
        "Fingerprinting algorithm to use for the schema to deserialise message. " +
            "This parameter will be ignored if schemaFingerPrint isn't set."
    )
    val schemaFingerPrintAlgorithm: String?,
)

fun NormalOpenAPIRoute.deserialise(
    schemaRegistry: SchemaRegistry,
    messageEnvelopeOptions: MessageSchemaOptions
) {
    val deserialiser = schemaRegistry.getDeserialiser()
    val envelopeDeserialiser = schemaRegistry.getEnvelopeDeserialser()
    post<DeserialiseParams, JsonObject, RawAvro>(
        info("Deserialise payload", "Turn binary AVRO payload into JSON"),
    ) { params, payload ->
        payload.stream.use {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    it.readAllBytes()
                } catch (e: Exception) {
                    throw BadRequestException("Cannot read binary input")
                }
            }
            // TODO: refactor/tidy this up, reduce duplication and complexity.
            if (null != messageEnvelopeOptions.envelopeSchemaName) {
                // use message envelope
                val envelopeSchema = messageEnvelopeOptions.envelopeSchemaName!!
                val payloadField = messageEnvelopeOptions.payloadFieldInEnvelope
                    ?: throw BadRequestException("--envelope-payload-field must be set.")
                respond(
                    responsePayload(
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
                )
            } else if (null == params.schema && null != params.schemaFingerPrintAlgorithm) {
                val fingerprintBin = Base64.getDecoder().decode(params.schemaFingerPrint)
                val fingerprintAlgo = params.schemaFingerPrint ?: SchemaRegistry.DEFAULT_FINGERPRINT_ALGORITHM
                payload.stream.use {
                    respond(
                        responsePayload(
                            deserialiser.decode(
                                bytes,
                                schemaRegistry.getByFingerPrint(fingerprintBin, fingerprintAlgo)?.fullName
                                    ?: throw BadRequestException(
                                        "Schema for fingerprint " +
                                            "[$fingerprintAlgo]${params.schemaFingerPrint} cannot be found."
                                    )
                            )
                        )
                    )
                }
            } else if (null != params.schema) {
                payload.stream.use {
                    respond(
                        responsePayload(deserialiser.decode(bytes, params.schema))
                    )
                }
            } else {
                // fall back on trying all schemas
                payload.stream.use {
                    respond(
                        responsePayload(deserialiser.decode(bytes))
                    )
                }
            }
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
