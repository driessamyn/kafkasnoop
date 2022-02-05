package kafkasnoop.routes

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory

fun Route.messages(kafkaClientFactory: KafkaClientFactory) {

    webSocket("/ws/{topic}") {
        call.run {
            val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")

            // todo: take partition, limit and offset from query string
            val offset = 0L

            kafkaClientFactory.createConsumer().use { consumer ->
                MessageProcessor(consumer, topicName, offset)
                    .startProcess().forEach {
                        send(
                            Frame.Text(it.toString())
                        )
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

            kafkaClientFactory.createConsumer().use { consumer ->
                val msgs = MessageProcessor(consumer, topicName, offset)
                    .startProcess(maxMsg).toList()
                respond(msgs)
            }
        }
    }
}
