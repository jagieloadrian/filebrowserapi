package com.anjo.filebrowser.ui.trash

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

class TrashScreenTest :
    FunSpec({
        context("emptyTrashText") {
            withTests(
                nameFn = { "should count ${it.first} trashed items" },
                1 to "1 item will be deleted forever. This cannot be undone.",
                4 to "All 4 items will be deleted forever. This cannot be undone.",
            ) { (count, expected) ->
                emptyTrashText(count) shouldBe expected
            }
        }
    })
