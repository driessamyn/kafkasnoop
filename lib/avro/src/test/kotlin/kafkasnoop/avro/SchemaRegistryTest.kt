package kafkasnoop.avro

import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaRegistryTest {

    val simpleSchemaResources = listOf("schemas/simple/car.avsc", "schemas/simple/superhero.avsc")
    val parser = Schema.Parser()
    val schemas = simpleSchemaResources.map {
        this::class.java.getResourceAsStream("/$it").use { s -> parser.parse(s) }
    }
    val registry = SchemaRegistry(schemas)
    val carSchema = schemas.single { it.fullName == "kafkasnoop.avro.Car" }
    val carFingerPrint = SchemaNormalization.parsingFingerprint("SHA-256", carSchema)

    @Test
    fun `when all return all`() {
        assertThat(registry.all).containsAll(schemas)
    }

    @Test
    fun `when get and exist return`() {
        assertThat(registry.getByName("kafkasnoop.avro.Car"))
            .isEqualTo(schemas.single { it.fullName == "kafkasnoop.avro.Car" })
    }

    @Test
    fun `when get and not exist return null`() {
        assertThat(registry.getByName("kafkasnoop.avro.Foo"))
            .isNull()
    }

    @Test
    fun `when get by fingerprint and exist return`() {
        assertThat(registry.getByFingerPrint(carFingerPrint))
            .isEqualTo(carSchema)
        assertThat(registry.getByFingerPrint(carFingerPrint, "SHA-256"))
            .isEqualTo(carSchema)
    }

    @Test
    fun `when get by fingerprint and not exist return null`() {
        assertThat(registry.getByFingerPrint(carFingerPrint + Byte.MIN_VALUE))
            .isNull()
    }
}
