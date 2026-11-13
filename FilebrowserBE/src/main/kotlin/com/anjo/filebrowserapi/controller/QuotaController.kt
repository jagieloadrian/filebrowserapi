package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.Quota
import com.anjo.filebrowserapi.service.QuotaManager
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
class QuotaController(
    private val quotaManager: QuotaManager,
) {
    @GetMapping("/api/v1/quota")
    fun quota(): Quota = quotaManager.quota()
}
