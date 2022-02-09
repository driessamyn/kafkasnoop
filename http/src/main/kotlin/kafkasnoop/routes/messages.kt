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

            val processor = MessageProcessor(kafkaClientFactory, topicName, offset)
            val partitions = processor.partitions

            // TODO: fix this up so that it streams from multiple partitions in parallel.
            processor
                .startProcess(partitions.first())
                .forEach {
                    logger.debug("Sending $it")
                    send(
                        Frame.Text(it.toString())
                    )
                }
        }
    }

    get("/api/{topic}") {
        call.run {
            val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")

            // todo: take partition, limit and offset from query string
            val maxMsg = 10
            val offset = 0L

            val processor = MessageProcessor(kafkaClientFactory, topicName, offset)
            val msgs = processor.partitions.map { p ->
                processor.startProcess(p, maxMsg).toList().sortedBy { it.offset }
            }.flatten().sortedByDescending { it.timestamp }
            respond(msgs)
        }
    }
}
