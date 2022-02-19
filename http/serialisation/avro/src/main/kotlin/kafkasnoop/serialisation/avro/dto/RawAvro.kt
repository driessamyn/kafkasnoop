package kafkasnoop.serialisation.avro.dto

import com.papsign.ktor.openapigen.content.type.binary.BinaryRequest
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import kafkasnoop.http.ContentType
import java.io.InputStream

@BinaryRequest([ContentType.AVRO])
@BinaryResponse([ContentType.AVRO])
data class RawAvro(val stream: InputStream)
