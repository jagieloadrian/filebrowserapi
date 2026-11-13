package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.Quota
import com.anjo.filebrowserapi.service.QuotaManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(QuotaController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class QuotaControllerTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: QuotaManager

    init {
        test("should return used bytes and limit") {
            // given
            every { service.quota() } returns Quota(6, 10)

            // when
            val response = mockMvc.get("/api/v1/quota").andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldBe """{"used":6,"limit":10}"""
        }
    }
}
