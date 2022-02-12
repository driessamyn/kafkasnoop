package kafkasnoop.dto

import com.papsign.ktor.openapigen.annotations.Response
import java.time.Instant

@Response("A Kafka Message")
data class Message(val offset: Long, val partition: String, val key: String, val value: String, val timestamp: Instant) {
    override fun toString(): String {
        return "$offset|$partition|$key|$value|$timestamp"
    }
}
