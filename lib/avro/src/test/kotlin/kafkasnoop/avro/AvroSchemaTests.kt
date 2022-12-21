package kafkasnoop.avro

import io.mockk.mockk
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AvroSchemaTests {

    @Test
    fun `when simple schema needs nothing`() {
        val schema = """
            {
              "type" : "record",
              "name" : "engine.avsc",
              "namespace" : "kafkasnoop.avro",
              "fields" : [ {
                "name" : "size",
                "type" : "int"
              }, {
                "name" : "fuelType",
                "type" : "string"
              } ]
            }
        """.trimIndent()

        val schemaObj = AvroSchema.create(schema)

        SoftAssertions().apply {
            assertThat(schemaObj.schema).isEqualTo(schema)
            assertThat(schemaObj.fullName).isEqualTo("kafkasnoop.avro.engine.avsc")
            assertThat(schemaObj.needs).isEmpty()
        }.assertAll()
    }

    @Test
    fun `when complex schema needs nothing`() {
        val schema = """
            {
              "type" : "record",
              "name" : "Things",
              "namespace" : "kafkasnoop.avro",
              "fields" : [ {
                "name" : "car",
                "type" : "kafkasnoop.avro.Car"
              }, {
                "name" : "engine",
                "type" : "kafkasnoop.avro.engine.avsc"
              } ]
            }
        """.trimIndent()

        val schemaObj = AvroSchema.create(schema)

        SoftAssertions().apply {
            assertThat(schemaObj.schema).isEqualTo(schema)
            assertThat(schemaObj.fullName).isEqualTo("kafkasnoop.avro.Things")
            assertThat(schemaObj.needs).containsAll(
                listOf(
                    "kafkasnoop.avro.Car", "kafkasnoop.avro.engine.avsc"
                )
            )
        }.assertAll()
    }

    @Test
    fun `when fields missing needs nothing`() {
        val schema = """
                {
                  "type": "enum",
                  "name": "Foo",
                  "namespace": "kafkasnoop.avro",
                  "symbols": [ "BAR", "UNKNOWN" ]
                }
        """.trimIndent()

        val schemaObj = AvroSchema.create(schema)

        SoftAssertions().apply {
            assertThat(schemaObj.needs).isEmpty()
        }.assertAll()
    }
}
