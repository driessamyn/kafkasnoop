package kafkasnoop.dto

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal var formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))

data class Message(val offset: Long, val partition: String, val key: String, val value: String, val timestamp: Long) {
    override fun toString(): String {
        return "$offset|$partition|$key|$value|${formatter.format(Instant.ofEpochMilli(timestamp))}"
    }
}
