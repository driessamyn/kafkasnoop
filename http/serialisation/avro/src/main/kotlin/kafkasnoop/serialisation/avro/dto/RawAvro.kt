package kafkasnoop.serialisation.avro.dto

import com.papsign.ktor.openapigen.content.type.binary.BinaryRequest
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import java.io.InputStream

const val CONTENT_TYPE_AVRO = "avro/binary"

@BinaryRequest([CONTENT_TYPE_AVRO])
@BinaryResponse([CONTENT_TYPE_AVRO])
data class RawAvro(val stream: InputStream)