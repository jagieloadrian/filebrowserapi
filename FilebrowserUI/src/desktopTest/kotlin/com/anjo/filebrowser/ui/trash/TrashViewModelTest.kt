package com.anjo.filebrowser.ui.trash

import com.anjo.filebrowser.ui.FakeFilebrowserApi
import com.anjo.filebrowser.ui.api.ApiException
import com.anjo.filebrowser.ui.api.ApiStatus
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.trashed
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest :
    FunSpec({
        lateinit var api: FakeFilebrowserApi
        lateinit var viewModel: TrashViewModel
        val older = trashed("1", "/a.txt", "2026-11-12T10:00:00Z")
        val newer = trashed("2", "/Docs/b.txt", "2026-11-13T10:00:00Z")

        beforeTest {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            api = FakeFilebrowserApi()
            api.trashItems += listOf(older, newer)
            viewModel = TrashViewModel(api)
        }

        afterTest { Dispatchers.resetMain() }

        test("should list the newest deleted items first") {
            // when
            viewModel.refresh()

            // then
            viewModel.state.value.items shouldBe Loadable.Ready(listOf(newer, older))
        }

        test("should show the error message when the trash cannot be loaded") {
            // given
            api.failures["trash"] = ApiException(ApiStatus.FAILED, "boom")

            // when
            viewModel.refresh()

            // then
            viewModel.state.value.items shouldBe Loadable.Failed(ApiStatus.FAILED.fallback)
        }

        test("should restore the item and reload") {
            // when
            viewModel.restore(older)

            // then
            api.calls.dropWhile { it != "restore 1" } shouldContain "trash"
            viewModel.snackbar.first().message shouldBe "Restored a.txt"
        }

        test("should delete one item forever") {
            // when
            viewModel.deleteForever(newer)

            // then
            api.calls shouldContain "deleteForever 2"
            viewModel.snackbar.first().message shouldBe "Deleted b.txt forever"
        }

        test("should empty the trash") {
            // when
            viewModel.emptyTrash()

            // then
            api.calls shouldContain "emptyTrash"
            viewModel.snackbar.first().message shouldBe "Trash emptied"
        }

        test("should show the server message when restoring fails") {
            // given
            api.failures["restore"] = ApiException(ApiStatus.CONFLICT, "/a.txt already exists")

            // when
            viewModel.restore(older)

            // then
            viewModel.snackbar.first().message shouldBe "/a.txt already exists"
            viewModel.state.value.working shouldBe 0
        }
    })
