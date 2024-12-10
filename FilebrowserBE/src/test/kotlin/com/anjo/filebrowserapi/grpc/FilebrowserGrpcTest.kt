package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.test.Test

@SpringBootTest
@TestPropertySource(properties = ["spring.profiles.active=server"])
class FilebrowserGrpcTest {

    @Autowired
    private lateinit var service: FilebrowserGrpc

    @Test
    fun `given request when showFilesTree then return empty list`() = runTest {
        //given
        val request = Filebrowser.PathRequest.newBuilder()
                .setPath("")
                .build()

        //when
        val actual = service.showFilesTree(request)

        //then
        actual.directoriesCount shouldBe 0
    }
}