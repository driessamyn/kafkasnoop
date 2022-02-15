package kafkasnoop.serialisation.avro.routes

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.features.*
import kafkasnoop.avro.Deserialiser
import kafkasnoop.avro.SchemaRegistry
import kafkasnoop.serialisation.avro.dto.RawAvro
import java.util.Base64

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

fun NormalOpenAPIRoute.deserialise(schemaRegistry: SchemaRegistry, deserialiser: Deserialiser) {
    post<DeserialiseParams, JsonObject, RawAvro>(
        info("Deserialise payload", "Turn binary AVRO payload into JSON"),
    ) { params, payload ->
        // TODO: tidy this up, reduce duplication and complexity.
        if (null == params.schema && null != params.schemaFingerPrintAlgorithm) {
            val fingerprintBin = Base64.getDecoder().decode(params.schemaFingerPrint)
            val fingerprintAlgo = params.schemaFingerPrint ?: SchemaRegistry.DEFAULT_FINGERPRINT_ALGORITHM
            payload.stream.use {
                respond(
                    JsonObject().apply {
                        add(
                            "schemas",
                            JsonArray().apply {
                                deserialiser.decode(
                                    it.readAllBytes(),
                                    schemaRegistry.getByFingerPrint(fingerprintBin, fingerprintAlgo)?.fullName
                                        ?: throw BadRequestException(
                                            "Schema for fingerprint " +
                                                    "[$fingerprintAlgo]${params.schemaFingerPrint} cannot be found."
                                        )
                                )
                            }
                        )
                    }
                )
            }
        } else if (null != params.schema) {
            payload.stream.use {
                respond(
                    JsonObject().apply {
                        add(
                            "schemas",
                            JsonArray().apply {
                                deserialiser.decode(it.readAllBytes(), params.schema)
                            }
                        )
                    }
                )
            }
        } else {
            // fall back on trying all schemas
            payload.stream.use {
                respond(
                    JsonObject().apply {
                        add("schemas", deserialiser.decode(it.readAllBytes()))
                    }
                )
            }
        }
    }
}
