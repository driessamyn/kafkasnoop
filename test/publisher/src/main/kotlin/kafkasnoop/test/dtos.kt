package kafkasnoop.test

import com.github.avrokotlin.avro4k.AvroFixed
import kotlinx.serialization.Serializable

@Serializable
data class Car(val make: String, val model: String, val coolFactor: Int, val engine: Engine)

@Serializable
data class Engine(val fuelType: String, val size: Int)

@Serializable
data class Superhero(val name: String, val coolFactor: Int)

@Serializable
data class EnvelopeWithFingerPrint(@AvroFixed(32) val schemaFingerprint: ByteArray, val payload: ByteArray)

@Serializable
data class EnvelopeWithFingerPrintAndAlgorithm(val fingerPrintAndAlgorithm: String, val schemaFingerprint: ByteArray, val payload: ByteArray)

@Serializable
data class EnvelopeWithSchemaName(val schemaName: String, val payload: ByteArray)
