package com.anjo.filebrowser.ui.validation

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class NamesTest :
    FunSpec({
        context("should reject names the backend refuses") {
            withTests(nameFn = { "'$it'" }, "", " ", "a/b") { name ->
                nameError(name).shouldNotBeNull()
            }
        }

        context("should accept other names") {
            withTests(nameFn = { "'$it'" }, "report.txt", "My folder", ".hidden") { name ->
                nameError(name).shouldBeNull()
            }
        }
    })
