package kafkasnoop.avro

import io.mockk.mockk
import io.mockk.verify
import org.apache.avro.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaLoaderTests {
    companion object {
        // Cannot use virtual FS because file walker
        @TempDir
        @JvmStatic
        lateinit var tempDir: Path
    }

    val schemaParser = Schema.Parser()
    val simpleSchemaResources = listOf("schemas/simple/car.avsc", "schemas/simple/superhero.avsc")
    val simpleSchemas by lazy {
        simpleSchemaResources.map { f ->
            schemaParser.parse(Files.readString(tempDir.resolve(f)))
        }
    }

    val complexSchemaResources = listOf(
        // reverse order of dependencies
        "schemas/complex/things.avsc",
        "schemas/complex/car.avsc",
        "schemas/complex/engine.avsc",
    )

    val factory = mockk<SchemaRegistryFactory>(relaxed = true)

    @BeforeAll
    private fun setup() {
        Files.createDirectories(tempDir.resolve("schemas/simple/"))
        // copy all test files to virtual FS
        simpleSchemaResources.forEach {
            Files.copy(this::class.java.getResourceAsStream("/$it"), tempDir.resolve(it))
        }
        Files.createDirectories(tempDir.resolve("schemas/complex/"))
        complexSchemaResources.forEach {
            Files.copy(this::class.java.getResourceAsStream("/$it"), tempDir.resolve(it))
        }
    }

    @Test
    fun `when given directory scan`() {
        val loader = SchemaLoader(factory)
        loader.createFromDir(tempDir.resolve("schemas/simple/"))

        verify {
            factory.create(
                withArg {
                    assertThat(it).containsAll(simpleSchemas)
                }
            )
        }
    }

    @Test
    fun `when given files read`() {
        val loader = SchemaLoader(factory)
        loader.createFromSchemaFiles(simpleSchemaResources.map { tempDir.resolve(it) })

        verify {
            factory.create(
                withArg {
                    assertThat(it).containsAll(simpleSchemas)
                }
            )
        }
    }

    @Test
    fun `when given schemas parse`() {
        val loader = SchemaLoader(factory)
        loader.createFromSchemaSources(simpleSchemaResources.map { Files.readString(tempDir.resolve(it)) })

        verify {
            factory.create(
                withArg {
                    assertThat(it).containsAll(simpleSchemas)
                }
            )
        }
    }

    @Test
    fun `when given schemas that reference others parse in order`() {
        val loader = SchemaLoader(factory)
        loader.createFromSchemaSources(complexSchemaResources.map { Files.readString(tempDir.resolve(it)) })

        verify {
            factory.create(
                withArg { arg ->
                    assertThat(arg.map { it.fullName }).containsAll(
                        listOf(
                            "kafkasnoop.avro.Car",
                            "kafkasnoop.avro.Engine",
                            "kafkasnoop.avro.Things",
                        )
                    )
                }
            )
        }
    }
}
