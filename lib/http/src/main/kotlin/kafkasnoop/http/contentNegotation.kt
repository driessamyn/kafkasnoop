package kafkasnoop.http

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import java.time.Instant

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            disableHtmlEscaping()
            registerTypeAdapter(
                Instant::class.java,
                InstantJsonSerialiser()
            )
        }
    }
}
