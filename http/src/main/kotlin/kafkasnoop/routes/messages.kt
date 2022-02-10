package kafkasnoop.routes

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

fun Route.messages(kafkaClientFactory: KafkaClientFactory) {

    webSocket("/ws/{topic}") {
        call.run {

            val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")
            // todo: take partition, limit and offset from query string
            val offset = 0L

            MessageProcessor(kafkaClientFactory, topicName, offset).use { processor ->
                val partitions = processor.partitions

                // TODO: fix this up so that it exists properly when the WS disconnects.
                val job = CoroutineScope(Dispatchers.Default).launch {
                    partitions.map { p ->
                        logger.info("Start processing from $p")
                        processor.startProcess(p).asFlow()
                    }.merge().cancellable().collect {
                        logger.debug("Sending $it")
                        send(
                            Frame.Text(it.toString())
                        )
                    }
                }
                try {
                    job.join()
                } catch (e: ClosedReceiveChannelException) {
                    logger.info("Websocket connecton closed")
                    job.cancel()
                } catch (e: Throwable) {
                    logger.error(e.message)
                    e.printStackTrace()
                    job.cancel()
                }
            }
        }
        logger.debug("Exit web socket")
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
