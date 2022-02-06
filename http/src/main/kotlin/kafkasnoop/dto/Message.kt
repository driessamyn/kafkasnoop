package kafkasnoop.dto

data class Message(val offset: Long, val partition: String, val key: String, val value: String) {
    override fun toString(): String {
        return "$offset|$partition|$key|$value"
    }
}
