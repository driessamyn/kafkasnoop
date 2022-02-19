package kafkasnoop.test

import com.github.ajalt.clikt.core.subcommands

@kotlinx.coroutines.ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    StartPublisher()
        .subcommands(SchemaCommand(), PublishCommand())
        .main(args)
}
