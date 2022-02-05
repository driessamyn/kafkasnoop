package kafkasnoop.dto

data class Message(val key: String, val value: String)

data class WebSocketMessage(val partition: String, val key: String, val value: String) {
    override fun toString(): String {
        return "$partition|$key|$value"
    }
}