package com.anjo.filebrowserapi.exception

import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatusCode

@ExtendWith(MockKExtension::class)
class GlobalControllerAdviceTest {

    @InjectMockKs
    private lateinit var advice: GlobalControllerAdvice

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }
    @Test
    fun `given error when handleCannotRealizeException then return responseEntity`() {
        //given
        val expectedCode = 422
        val exception = CannotRealizeException(TEST_MESSAGE)
        val expectedBody = ErrorResponse(exception.javaClass.simpleName, TEST_MESSAGE)

        //when
        val result = advice.handleCannotRealizeException(exception)

        //then
        assertSoftly {
            result.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            result.body shouldBe expectedBody
        }
    }

    @Test
    fun `given error when handleBadInputException then return responseEntity`() {
        //given
        val expectedCode = 400
        val exception = BadInputException(TEST_MESSAGE)
        val expectedBody = ErrorResponse(exception.javaClass.simpleName, TEST_MESSAGE)

        //when
        val result = advice.handleBadInputException(exception)

        //then
        assertSoftly {
            result.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            result.body shouldBe expectedBody
        }
    }
}