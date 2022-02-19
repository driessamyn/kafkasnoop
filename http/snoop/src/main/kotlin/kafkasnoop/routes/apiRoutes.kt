package kafkasnoop.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import kafkasnoop.KafkaClientFactory
import kafkasnoop.serialisation.MessageDeserialiser

fun NormalOpenAPIRoute.snoopApiRoutes(
    kafkaClientFactory: KafkaClientFactory,
    messageDeserialiser: MessageDeserialiser
) {
    route("/api").topics(kafkaClientFactory)
    route("/api/{topic}").messages(kafkaClientFactory, messageDeserialiser)
}
