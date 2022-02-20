package kafkasnoop.serialisation

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kafkasnoop.http.ContentType

class ByteToStringService(
    private val serviceUrl: Url,
    private val fallbackDeserialiser: MessageDeserialiser = SimpleByteToStringDeserialiser()
) : MessageDeserialiser {
    override suspend fun deserialise(payload: ByteArray): String {
        try {
            HttpClient(Java).use { client ->
                val response: HttpResponse = client.post(serviceUrl) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.AVRO)
                    }
                    body = payload
                }
                if(response.status.isSuccess())
                    return response.readText(Charsets.UTF_8)
                // when failing fall back to just simple
                return fallbackDeserialiser.deserialise(payload)
            }
        } catch (e: Exception) {
            // when failing fall back to just simple
            return fallbackDeserialiser.deserialise(payload)
        }
    }
}
