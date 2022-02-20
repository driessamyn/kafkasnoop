package kafkasnoop.test

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.io.AvroEncodeFormat
import kafkasnoop.test.PublishCommand.Companion.HERO_TOPIC
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import kotlin.random.Random

object ExamplesWithEnvelopeAndFingerPrint {
    private val logger = LoggerFactory.getLogger(ExamplesWithEnvelopeAndFingerPrint::class.java)

    private val envelopeSchema = Avro.default.schema(EnvelopeWithFingerPrint.serializer())
    private val heroSchema = Avro.default.schema(Superhero.serializer())
    private val heroSchemaFingerprint =
        SchemaNormalization.parsingFingerprint("SHA-256", heroSchema)

    fun produce(kafkaProducer: KafkaProducer<ByteArray, ByteArray>) {
        logger.info("Publish cars and heros with schema envelope.")

        var count = 0
        // TODO
        while (true) {
            val coolHero = Superhero("Hero-$count", Random.nextInt(0, 100))

            val heroBytes = ByteArrayOutputStream().use { bao ->
                Avro.default.openOutputStream(Superhero.serializer()) {
                    schema = heroSchema
                    encodeFormat = AvroEncodeFormat.Binary
                }.to(bao).write(coolHero).close()
                bao.toByteArray()
            }

            val envelope = EnvelopeWithFingerPrint(heroSchemaFingerprint, heroBytes)
            val valueBytes = ByteArrayOutputStream().use { bao ->
                Avro.default.openOutputStream(EnvelopeWithFingerPrint.serializer()) {
                    schema = envelopeSchema
                    encodeFormat = AvroEncodeFormat.Binary
                }.to(bao).write(envelope).close()
                bao.toByteArray()
            }

            kafkaProducer.send(
                ProducerRecord(HERO_TOPIC, coolHero.name.toByteArray(), valueBytes)
            )

            // TODO: suspend
            Thread.sleep(1000)

            count += 1

            if (count.mod(10) == 0) {
                logger.info("Published $count heros.")
            }
        }
    }
}
