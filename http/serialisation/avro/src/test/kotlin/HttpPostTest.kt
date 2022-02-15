import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.testing.*
import kafkasnoop.avro.Deserialiser
import kafkasnoop.avro.SchemaLoader
import kafkasnoop.serialisation.avro.dto.CONTENT_TYPE_AVRO
import kafkasnoop.serialisation.avro.serialisationServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class HttpPostE2ETest {
    companion object {
        val schemaDir = Paths.get("src", "test", "resources")
        val avroMsg = schemaDir.resolve("rover-mini.avro")
    }

    private val registry = SchemaLoader().createFromDir(schemaDir.toAbsolutePath())
    private val deserialiser = Deserialiser(registry)

    private val expectedMultipleResponse = Gson().toJson(
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
    private val expectedCarMessage = Gson().toJson(
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

    @Test
    fun `when post avro deserialise with all possible schemas`() {
        withTestApplication({
            serialisationServer(registry, deserialiser)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json?") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, CONTENT_TYPE_AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedMultipleResponse).isEqualTo(Gson().toJson(response.content))
            }
        }
    }

    @Test
    fun `when post avro deserialise with given schema`() {
        withTestApplication({
            serialisationServer(registry, deserialiser)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json?schema=kafkasnoop.avro.Car") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, CONTENT_TYPE_AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedCarMessage).isEqualTo(Gson().toJson(response.content))
            }
        }
    }

    @Test
    fun `when post avro deserialise with only possible schemas`() {
        withTestApplication({
            val registry2 = SchemaLoader().createFromSchemaFiles(listOf(schemaDir.resolve("car.avsc")))
            val deserialiser2 = Deserialiser(registry2)
            serialisationServer(registry2, deserialiser2)
        }) {
            with(
                handleRequest(HttpMethod.Post, "/json") {
                    val fileBytes = avroMsg.toFile()

                    addHeader(HttpHeaders.ContentType, CONTENT_TYPE_AVRO)
                    setBody(fileBytes.readBytes())
                }
            ) {
                assertThat(expectedCarMessage).isEqualTo(Gson().toJson(response.content))
            }
        }
    }
}
