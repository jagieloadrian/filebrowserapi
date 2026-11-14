package com.anjo.filebrowserapi.security

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpcKt
import io.grpc.ManagedChannelBuilder
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.isDirectory

@SpringBootTest(properties = ["spring.grpc.server.port=0"])
@ActiveProfiles("server", "local")
@ApplyExtension(SpringExtension::class)
class LocalGrpcProfileTest : FunSpec() {
    @LocalGrpcServerPort
    private var port: Int = 0

    private val channel by lazy { ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build() }

    init {
        afterSpec {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        test("should serve RPCs without token as user local when local profile is active") {
            // given
            val stub = FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub(channel)
            stub.addDirectory(
                Filebrowser.FileOperation
                    .newBuilder()
                    .setName("notes")
                    .build(),
            )

            // when
            val directories = stub.showDirectoriesTree(Filebrowser.PathRequest.getDefaultInstance()).directoriesList

            // then
            directories shouldContain "/notes"
            root.resolve("filebrowser/local/notes").isDirectory() shouldBe true
        }
    }

    companion object {
        private val root: Path = Files.createTempDirectory("filebrowser-local-grpc")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { root.toString() }
        }
    }
}
