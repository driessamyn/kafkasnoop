package kafkasnoop.routes

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
fun Route.messagesWs(kafkaClientFactory: KafkaClientFactory) {

    webSocket("/ws/{topic}") {
        val topicName = call.parameters["topic"] ?: throw IllegalArgumentException("Topic must be provided")
        val partitionFilter = call.parameters["partition"]?.toInt()
        val minOffset = call.parameters["minOffset"]?.toLong() ?: 0L

        MessageProcessor(kafkaClientFactory, topicName).use { processor ->
            val partitions = processor.partitions

            val job = CoroutineScope(Dispatchers.Default).launch {
                partitions
                    .filter { null == partitionFilter || it.partition() == partitionFilter }
                    .map { p ->
                        logger.info("Start processing from $p")
                        processor.startProcess(p, minOffset = minOffset).asFlow().cancellable().catch {
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
}
