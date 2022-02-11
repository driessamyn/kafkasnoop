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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

fun Route.messages(kafkaClientFactory: KafkaClientFactory) {

    webSocket("/ws/{topic}") {
        val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")
        // todo: take partition, limit and offset from query string
        val offset = 0L

        MessageProcessor(kafkaClientFactory, topicName, offset).use { processor ->
            val partitions = processor.partitions

            val job = CoroutineScope(Dispatchers.Default).launch {
                partitions.map { p ->
                    logger.info("Start processing from $p")
                    processor.startProcess(p).asFlow().cancellable().catch {
                        logger.error(it.message)
                    }
                }.merge().collect {
                    logger.debug("Sending $it")
                    send(
                        Frame.Text(it.toString())
                    )
                }
            }
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Close -> {
                            job.cancel()
                        }
                        else -> logger.warn("Incoming Frame type of ${frame.frameType} not supported.")
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.info("WebSocket connection unexpectedly closed")
            } catch (e: Throwable) {
                logger.error(e.message)
                e.printStackTrace()
            } finally {
                job.cancel()
                logger.debug("WebSocket terminated")
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
