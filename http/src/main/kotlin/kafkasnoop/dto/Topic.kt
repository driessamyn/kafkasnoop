package kafkasnoop.dto

import com.papsign.ktor.openapigen.annotations.Response

@Response("Kafka Topic Partition")
data class Partition(
    val index: Int,
    val beginOffset: Long,
    val endOffset: Long,
    val inSyncReplicas: Int,
    val offlineReplicas: Int,
)

@Response("Kafka Topic")
data class Topic(val name: String, val partitions: List<Partition>)
