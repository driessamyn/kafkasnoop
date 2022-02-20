import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kafkasnoop.avro.AvroSerialisationException
import kafkasnoop.avro.SchemaLoader
import kafkasnoop.http.ContentType
import kafkasnoop.serialisation.avro.MessageSchemaOptions
import kafkasnoop.serialisation.avro.serialisationServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class SimpleDeserialisationE2ETest {
    companion object {
        val resourceDir = Paths.get("src", "test", "resources").toAbsolutePath()
        val schemaDir = resourceDir.resolve("schema").resolve("simple")
        val avroMsg = resourceDir.resolve("rover-mini.avro")
    }

    private val registry = SchemaLoader().createFromDir(schemaDir)

    private val expectedMultipleResponse = JsonParser.parseString(
        """
        {
          "schemas": [
            {
              "schema": "kafkasnoop.avro.Superhero",
              "message": {
                "name": "Rover",
                "coolFactor": 4
              }
            },
            {
              "schema": "kafkasnoop.avro.Car",
              "message": {
                "make": "Rover",
                "model": "Mini",
                "coolFactor": 5,
                "engine": {
                  "size": 1300,
                  "fuelType": "petrol"
                }
              }
            }
          ]
        }
        """.trimIndent()
    )
    private val expectedCarMessage = JsonParser.parseString(
        """
        {
          "schemas": [
            {
              "schema": "kafkasnoop.avro.Car",
              "message": {
                "make": "Rover",
                "model": "Mini",
                "coolFactor": 5,
                "engine": {
                  "size": 1300,
                  "fuelType": "petrol"
                }
              }
            }
          ]
        }
        """.trimIndent()
    )

    val envelopeOptions = mockk<MessageSchemaOptions>(relaxed = true) {
        every { envelopeSchemaName } returns null
        every { schemaNameFieldInEnvelope } returns null
        every { schemaFingerPrintFieldInEnvelope } returns null
    }

    @Test
    fun `when post avro deserialise with all possible schemas`() {
        withTestApplication({
            serialisationServer(registry, envelopeOptions)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedMultipleResponse).isEqualTo(JsonParser.parseString(response.content))
            }
        }
    }

    @Test
    fun `when post avro deserialise with given schema`() {
        withTestApplication({
            serialisationServer(registry, envelopeOptions)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json?schema=kafkasnoop.avro.Car") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedCarMessage).isEqualTo(JsonParser.parseString(response.content))
            }
        }
    }

    @Test
    fun `when post avro deserialise with no matching schemas return empty`() {
        withTestApplication({
            val registry2 = SchemaLoader().createFromSchemaFiles(listOf(schemaDir.resolve("foo.avsc")))
            serialisationServer(registry2, envelopeOptions)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(JsonParser.parseString(response.content).asJsonObject.get("schemas").asJsonArray).isEmpty()
            }
        }
    }

    @Test
    fun `when post avro deserialise with invalid schema throw`() {
        withTestApplication({
            serialisationServer(registry, envelopeOptions)
        }) {
            with(
                assertThrows<AvroSerialisationException> {
                    handleRequest(HttpMethod.Post, "/json?schema=kafkasnoop.avro.Foo") {
                        val fileBytes = avroMsg.toFile()

                        addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                        setBody(fileBytes.readBytes())
                    }
                }
            ) {}
        }
    }

    @Test
    fun `when post avro deserialise with unknown schema throw`() {
        withTestApplication({
            serialisationServer(registry, envelopeOptions)
        }) {
            with(
                assertThrows<AvroSerialisationException> {
                    handleRequest(HttpMethod.Post, "/json?schema=kafkasnoop.avro.FooBar") {
                        val fileBytes = avroMsg.toFile()

                        addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                        setBody(fileBytes.readBytes())
                    }
                }
            ) {}
        }
    }

    // TODO: add flag to allow case insensitive match
    @Test
    fun `when post avro deserialise with case insensitive match throw`() {
        withTestApplication({
            serialisationServer(registry, envelopeOptions)
        }) {
            with(
                assertThrows<AvroSerialisationException> {
                    handleRequest(HttpMethod.Post, "/json?schema=kafkasnoop.avro.car") {
                        val fileBytes = avroMsg.toFile()

                        addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                        setBody(fileBytes.readBytes())
                    }
                }
            ) {}
        }
    }

    @Test
    fun `when post avro deserialise with only possible schemas`() {
        withTestApplication({
            val registry2 = SchemaLoader().createFromSchemaFiles(listOf(schemaDir.resolve("car.avsc")))
            serialisationServer(registry2, envelopeOptions)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, ContentType.AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedCarMessage).isEqualTo(JsonParser.parseString(response.content))
            }
        }
    }
}
