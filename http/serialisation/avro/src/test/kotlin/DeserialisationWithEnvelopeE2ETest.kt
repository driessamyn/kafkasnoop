import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kafkasnoop.avro.SchemaLoader
import kafkasnoop.http.ContentType
import kafkasnoop.serialisation.avro.MessageSchemaOptions
import kafkasnoop.serialisation.avro.serialisationServer
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeserialisationWithEnvelopeE2ETest {
    companion object {
        val resourceDir = Paths.get("src", "test", "resources").toAbsolutePath()
        val schemaDir = resourceDir.resolve("schema").resolve("with-envelope")
    }

    private val registry = SchemaLoader().createFromDir(schemaDir)

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

    private val expectedHeroMessage = JsonParser.parseString(
        """
        {
          "schemas": [
            {
              "schema": "kafkasnoop.avro.Superhero",
               "message": {
                 "name": "Batman",
                  "coolFactor": 9
               }
            }
          ]
       }
        """.trimIndent()
    )

    @Test
    fun `when deserialise with schema name in envelope`() {
        val envelopeOptions = mockk<MessageSchemaOptions>(relaxed = true) {
            every { envelopeSchemaName } returns "kafkasnoop.avro.EnvelopeWithSchemaName"
            every { schemaNameFieldInEnvelope } returns "schemaName"
            every { payloadFieldInEnvelope } returns "payload"
        }

        val avroMsg = resourceDir.resolve("car-with-envelope-name.avro")

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
                Assertions.assertThat(JsonParser.parseString(response.content)).isEqualTo(expectedCarMessage)
            }
        }
    }

    @Test
    fun `when deserialise with schema fingerprint in envelope`() {
        val envelopeOptions = mockk<MessageSchemaOptions>(relaxed = true) {
            every { envelopeSchemaName } returns "kafkasnoop.avro.EnvelopeWithFingerPrint"
            every { schemaNameFieldInEnvelope } returns null
            every { schemaFingerPrintFieldInEnvelope } returns "schemaFingerprint"
            every { payloadFieldInEnvelope } returns "payload"
            every { schemaFingerPrintAlgorithmFieldInEnvelope } returns null
            every { schemaFingerPrintAlgorithm } returns "SHA-256"
        }

        val avroMsg = resourceDir.resolve("car-with-envelope-fingerprint.avro")

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
                Assertions.assertThat(JsonParser.parseString(response.content)).isEqualTo(expectedCarMessage)
            }
        }
    }

    @Test
    fun `when deserialise with schema fingerprint and algorithm in envelope`() {
        val envelopeOptions = mockk<MessageSchemaOptions>(relaxed = true) {
            every { envelopeSchemaName } returns "kafkasnoop.avro.EnvelopeWithFingerPrintAndAlgorithm"
            every { schemaNameFieldInEnvelope } returns null
            every { schemaFingerPrintFieldInEnvelope } returns "schemaFingerprint"
            every { payloadFieldInEnvelope } returns "payload"
            every { schemaFingerPrintAlgorithmFieldInEnvelope } returns "fingerPrintAndAlgorithm"
            every { schemaFingerPrintAlgorithm } returns "Foo"
        }

        val avroMsg = resourceDir.resolve("hero-with-fingerprint-algo.avro")

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
                Assertions.assertThat(JsonParser.parseString(response.content)).isEqualTo(expectedHeroMessage)
            }
        }
    }
}
