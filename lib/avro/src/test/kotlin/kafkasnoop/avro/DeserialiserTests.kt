package kafkasnoop.avro

import com.google.gson.JsonParser
import io.mockk.every
import io.mockk.mockk
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeserialiserTests {

    val parser = Schema.Parser()
    val carSchema = this::class.java
        .getResourceAsStream("/schemas/simple/car-with-engine.avsc").use { parser.parse(it) }
    val superheroSchema = this::class.java
        .getResourceAsStream("/schemas/simple/superhero.avsc").use { parser.parse(it) }
    val carMessage = this::class.java
        .getResourceAsStream("/messages/rover-mini.avro").use { it.readAllBytes() }

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

    val schemaRegistry = mockk<SchemaRegistry>() {
        every { getByName(carSchema.fullName) } returns carSchema
        every { getByName(superheroSchema.fullName) } returns superheroSchema
        every { all }.returns(listOf(carSchema, superheroSchema))
    }

    val deserialiser = Deserialiser(schemaRegistry)

    @Test
    fun `when decode with schema use`() {
        val mini = deserialiser.decode(carMessage, carSchema.fullName)

        assertThat(mini).isEqualTo(JsonParser.parseString(carJson).asJsonObject)
    }

    @Test
    fun `when decode without schema try all`() {
        val decoded = deserialiser.decode(carMessage)

        // successfully deserialise to Car
        assertThat(decoded.map { it }).contains(
            JsonParser.parseString(carJson).asJsonObject,
        )

        // can also serialise to Superhero
        assertThat(
            decoded.filter {
                it.asJsonObject.get("schema").asString == "kafkasnoop.avro.Superhero"
            }
        ).isNotEmpty
    }

    @Test
    fun `when invalid msg return empty`() {
        val decoded = deserialiser.decode("".toByteArray())

        assertThat(decoded).isEmpty()
    }

    @Test
    fun `when invalid msg and schema throw`() {
        assertThrows<AvroSerialisationException> {
            deserialiser.decode("".toByteArray(), carSchema.fullName)
        }
    }
}
