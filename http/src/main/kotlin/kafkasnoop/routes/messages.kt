package kafkasnoop.routes

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import kafkasnoop.dto.WebSocketMessage

fun Route.messages(kafkaClientFactory: KafkaClientFactory) {

    webSocket("/ws/{topic}") {
        call.run {
            val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")

            // todo: take partition, limit and offset from query string
            val offset = 0L

            val msgProcessor = MessageProcessor(kafkaClientFactory, topicName, offset)

            while (true) {
                msgProcessor.partitions.forEach { p ->
                    msgProcessor.getMessages(p).forEach { m ->
                        send(
                            Frame.Text(WebSocketMessage(p.toString(), m.key, m.value).toString())
                        )
                    }
                }
            }
        }
    }

    get("/api/{topic}") {
        call.run {
            val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")

            // todo: take partition, limit and offset from query string
            val maxMsg = 100
            val offset = 0L

            val msgProcessor = MessageProcessor(kafkaClientFactory, topicName, offset)

            kafkaClientFactory.getOrCreateConsumer().let { consumer ->
                val endOffsets = consumer.endOffsets(msgProcessor.partitions)

                val response = msgProcessor.partitions.map { p ->
                    val messages = mutableListOf<Message>()
                    val latestOffset = endOffsets[p]?.minus(1) ?: 0
                    while (messages.count() < maxMsg && consumer.position(p) < latestOffset) {
                        messages.addAll(msgProcessor.getMessages(p))
                    }

                    p to messages
                }
                respond(response.toMap())
            }
        }
    }
}
