package kafkasnoop.avro

import com.google.gson.JsonParser
import org.apache.avro.Schema
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnvelopeDeserialiserTest {
    val parser = Schema.Parser()
    val carSchema = this::class.java
        .getResourceAsStream("/schemas/simple/car-with-engine.avsc").use { parser.parse(it) }
    val superheroSchema = this::class.java
        .getResourceAsStream("/schemas/simple/superhero.avsc").use { parser.parse(it) }
    val envelopeWithNameSchema = this::class.java
        .getResourceAsStream("/schemas/complex/envelope-name.avsc").use { parser.parse(it) }
    val envelopeWithFingerPrintSchema = this::class.java
        .getResourceAsStream("/schemas/complex/envelope-fingerprint.avsc").use { parser.parse(it) }
    val envelopeWithFingerPrintAndAlgorithmSchema = this::class.java
        .getResourceAsStream("/schemas/complex/envelope-fingerprint-algo.avsc").use { parser.parse(it) }

    val carWithNameEnvelopeMessage = this::class.java
        .getResourceAsStream("/messages/car-with-envelope-name.avro").use { it.readAllBytes() }
    val carWithFingerprintEnvelopeMessage = this::class.java
        .getResourceAsStream("/messages/car-with-envelope-fingerprint.avro").use { it.readAllBytes() }
    val heroWithFingerprintEnvelopeMessage = this::class.java
        .getResourceAsStream("/messages/hero-with-fingerprint-algo.avro").use { it.readAllBytes() }

    val carJson = """
        {
            "schema":"kafkasnoop.avro.Car",
            "message":{
                "make":"Rover",
                "model":"Mini",
                "coolFactor":5,
                "engine":{
                    "size":1300,
                    "fuelType":"petrol"
                }
            }
        }
    """.trimIndent()

    val heroJson = """
        {
            "schema":"kafkasnoop.avro.Superhero",
            "message":{
                "name":"Batman",
                "coolFactor":9
            }
        }
    """.trimIndent()

    val schemaRegistry = SchemaRegistry(
        listOf(carSchema, superheroSchema, envelopeWithNameSchema, envelopeWithFingerPrintSchema, envelopeWithFingerPrintAndAlgorithmSchema)
    )
    val deserialiserWithName = EnvelopeDeserialiser(schemaRegistry)

    @Test
    fun `when deserialise with envelope`() {
        val mini = deserialiserWithName.decode(
            carWithNameEnvelopeMessage,
            envelopeWithNameSchema.fullName,
            "schemaName",
            "payload"
        )

        Assertions.assertThat(mini).isEqualTo(JsonParser.parseString(carJson).asJsonObject)
    }

    @Test
    fun `when deserialise with envelope and fingerprint`() {
        val mini = deserialiserWithName.decodeFromFingerPrint(
            carWithFingerprintEnvelopeMessage,
            envelopeWithFingerPrintSchema.fullName,
            "schemaFingerprint",
            "payload"
        )

        Assertions.assertThat(mini).isEqualTo(JsonParser.parseString(carJson).asJsonObject)
    }

    @Test
    fun `when deserialise with envelope and fingerprint and algo`() {
        val mini = deserialiserWithName.decodeFromFingerPrint(
            carWithFingerprintEnvelopeMessage,
            envelopeWithFingerPrintSchema.fullName,
            "schemaFingerprint",
            "payload",
            schemaFingerPrintAlgorithm = "SHA-256"
        )

        Assertions.assertThat(mini).isEqualTo(JsonParser.parseString(carJson).asJsonObject)
    }

    @Test
    fun `when deserialise with envelope and fingerprint and algo field`() {
        val hero = deserialiserWithName.decodeFromFingerPrint(
            heroWithFingerprintEnvelopeMessage,
            envelopeWithFingerPrintAndAlgorithmSchema.fullName,
            "schemaFingerprint",
            "payload",
            schemaFingerPrintAlgoritmField = "fingerPrintAndAlgorithm"
        )

        Assertions.assertThat(hero).isEqualTo(JsonParser.parseString(heroJson).asJsonObject)
    }
}
