package kafkasnoop.routes

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.time.Instant

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

private const val MAX_MESSAGES_DEFAULT = 10
private const val MIN_OFFSET_DEFAULT = 0L
data class GetTopicMessagesParams(
    @PathParam("Name of the topic")
    val topic: String,
    @QueryParam("Partition filter (Optional)")
    val partition: Int?,
    @QueryParam("Maximum number of messages to return per partition - Optional, default: $MAX_MESSAGES_DEFAULT")
    val max: Int?,
    @QueryParam("Minimum offset to start returning messages from - Optional, default: $MIN_OFFSET_DEFAULT")
    val minOffset: Long?
)
fun NormalOpenAPIRoute.messageOpenApi(kafkaClientFactory: KafkaClientFactory) {
    get<GetTopicMessagesParams, List<Message>>(
        info("Messages", "Get Messages from given topoc"),
        example = listOf(
            Message(
                0,
                "topic-parition",
                "message-key",
                "message-value", Instant.now().toEpochMilli()
            )
        )
    ) { params ->

        MessageProcessor(kafkaClientFactory, params.topic).use { processor ->
            val msgs = processor.partitions
                .filter { null == params.partition || it.partition() == params.partition }
                .map { p ->
                    processor.startProcess(
                        p,
                        params.max ?: MAX_MESSAGES_DEFAULT,
                        params.minOffset ?: MIN_OFFSET_DEFAULT
                    ).toList().sortedBy { it.offset }
                }.flatten().sortedByDescending { it.timestamp }
            respond(msgs)
        }
    }
}
