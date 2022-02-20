package kafkasnoop.http

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class ServerOptionsImpl : ServerOptions,
    OptionGroup("Web Server Options") {
    override val port: Int by option("-p", "--port", help = "Port to expose KafkaSnoop on").int()
        .default(9000)
}
