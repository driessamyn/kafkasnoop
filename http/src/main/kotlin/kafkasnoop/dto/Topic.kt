package kafkasnoop.dto

data class Partition(val index: Int, val inSyncReplicas: Int, val offlineReplicas: Int)
data class Topic(val name: String, val partitions: List<Partition>)
