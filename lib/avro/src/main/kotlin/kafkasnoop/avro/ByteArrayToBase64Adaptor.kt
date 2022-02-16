package kafkasnoop.avro

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.Base64

class ByteArrayToBase64Adaptor : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
    private val encoder = Base64.getEncoder().withoutPadding()
    private val decoder = Base64.getDecoder()
    override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(encoder.encodeToString(src))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): ByteArray {
        return decoder.decode(json.asString)
    }
}
