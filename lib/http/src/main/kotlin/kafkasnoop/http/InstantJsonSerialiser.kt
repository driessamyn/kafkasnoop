package kafkasnoop.http

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class InstantJsonSerialiser : JsonSerializer<Instant> {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))

    override fun serialize(instant: Instant, typeOfSrc: Type, context: JsonSerializationContext):
        JsonElement {
        return JsonPrimitive(formatter.format(instant))
    }
}
