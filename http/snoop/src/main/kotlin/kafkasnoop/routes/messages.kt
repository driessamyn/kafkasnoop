package kafkasnoop.routes

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import java.time.Instant

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
fun NormalOpenAPIRoute.messages(kafkaClientFactory: KafkaClientFactory) {
    get<GetTopicMessagesParams, List<Message>>(
        info("Messages", "Get Messages from given topic"),
        example = listOf(
            Message(
                0,
                "topic-parition",
                "message-key",
                "message-value", Instant.now()
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
