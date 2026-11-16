package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.FakeFilebrowserApi
import com.anjo.filebrowser.ui.PNG_SIZE
import com.anjo.filebrowser.ui.api.ApiException
import com.anjo.filebrowser.ui.api.ApiStatus
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.folder
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.FilePreview
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.PreviewContent
import com.anjo.filebrowser.ui.model.PreviewLine
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.png
import com.anjo.filebrowser.ui.trashed
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private const val MIB = 1024L * 1024

private data class SizeCase(
    val name: String,
    val entry: Entry,
    val downloads: List<String>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelTest :
    FunSpec({
        val scheduler = TestCoroutineScheduler()
        lateinit var api: FakeFilebrowserApi
        lateinit var viewModel: FilesViewModel

        beforeTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
            api = FakeFilebrowserApi()
            api.entries += listOf(file("/b.txt"), file("/a.txt"), folder("/Docs"), file("/Docs/report.txt"))
            viewModel = FilesViewModel(api)
        }

        afterTest { Dispatchers.resetMain() }

        fun shownPaths() = (viewModel.state.value.entries as Loadable.Ready).value.map { it.path }

        context("listing") {
            test("should show the direct children of the folder with folders first and the quota") {
                // when
                viewModel.refresh()

                // then
                shownPaths() shouldBe listOf("/Docs", "/a.txt", "/b.txt")
                viewModel.state.value.quota shouldBe Quota(used = 10, limit = 100)
            }

            test("should show the children of an opened folder") {
                // when
                viewModel.open("/Docs")

                // then
                viewModel.state.value.path shouldBe "/Docs"
                shownPaths() shouldBe listOf("/Docs/report.txt")
            }

            test("should show the error message when the listing fails") {
                // given
                api.failures["list"] = ApiException(ApiStatus.OFFLINE)

                // when
                viewModel.refresh()

                // then
                viewModel.state.value.entries shouldBe Loadable.Failed(ApiStatus.OFFLINE.fallback)
            }
        }

        context("search") {
            test("should search below the folder by name after the typing pause") {
                // when
                viewModel.search("rep")
                viewModel.search("repo")
                scheduler.advanceTimeBy(SEARCH_DELAY_MS + 1)

                // then
                api.calls shouldContain "list  repo"
                api.calls shouldNotContain "list  rep"
                shownPaths() shouldBe listOf("/Docs/report.txt")
            }
        }

        context("upload") {
            test("should offer to replace an existing file and upload with overwrite when accepted") {
                // given
                viewModel.refresh()

                // when
                viewModel.upload(PickedFile("a.txt", byteArrayOf(1)))
                val conflict = viewModel.snackbar.first()
                conflict.onAction()

                // then
                conflict.message shouldBe "File a.txt already exists"
                conflict.action shouldBe "Replace"
                api.calls shouldContain "upload /a.txt true"
                viewModel.snackbar.first().message shouldBe "Uploaded a.txt"
                viewModel.state.value.working shouldBe 0
            }
        }

        context("delete") {
            test("should offer undo and restore the latest trashed item with the same path") {
                // given
                api.trashItems +=
                    listOf(
                        trashed("old", "/a.txt", "2026-11-12T10:00:00Z"),
                        trashed("new", "/a.txt", "2026-11-13T10:00:00Z"),
                        trashed("other", "/b.txt", "2026-11-14T10:00:00Z"),
                    )

                // when
                viewModel.delete(file("/a.txt"))
                val deleted = viewModel.snackbar.first()
                deleted.onAction()

                // then
                api.calls shouldContain "delete /a.txt"
                deleted.message shouldBe "Deleted a.txt"
                deleted.action shouldBe "Undo"
                api.calls shouldContain "restore new"
                viewModel.snackbar.first().message shouldBe "Restored a.txt"
            }

            test("should tell when the deleted item is no longer in the trash") {
                // when
                viewModel.delete(file("/a.txt"))
                viewModel.snackbar.first().onAction()

                // then
                viewModel.snackbar.first().message shouldBe ApiStatus.NOT_FOUND.fallback
            }
        }

        context("preview") {
            suspend fun previewed() =
                viewModel.state
                    .first { it.preview?.content != Loadable.Loading }
                    .preview
                    ?.content

            test("should show the whole text of a small text file") {
                // given
                api.contents["/a.txt"] = "hello".encodeToByteArray()

                // when
                viewModel.preview(file("/a.txt", size = 5))

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.Text(listOf(PreviewLine("hello")), shownBytes = null))
            }

            test("should replace invalid UTF-8 bytes with the replacement character") {
                // given
                api.contents["/a.txt"] = byteArrayOf(0x61, 0xFF.toByte(), 0x62)

                // when
                viewModel.preview(file("/a.txt", size = 3))

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.Text(listOf(PreviewLine("a\uFFFDb")), shownBytes = null))
            }

            context("should decide from the known size how much of the file to read") {
                withTests(
                    nameFn = { it.name },
                    SizeCase("text at the limit is read whole", file("/a.txt", size = MIB), listOf("download /a.txt")),
                    SizeCase("bigger text is read up to the limit", file("/a.txt", size = MIB + 1), listOf("download /a.txt $MIB")),
                    SizeCase("image at the limit is read whole", file("/a.png", size = 20 * MIB), listOf("download /a.png")),
                    SizeCase("bigger image is not read", file("/a.png", size = 20 * MIB + 1), emptyList()),
                ) { case ->
                    // when
                    viewModel.preview(case.entry)
                    previewed()

                    // then
                    api.calls.filter { it.startsWith("download") } shouldBe case.downloads
                }
            }

            test("should say how much of a big text file it shows") {
                // given
                api.contents["/a.txt"] = "head".encodeToByteArray()

                // when
                viewModel.preview(file("/a.txt", size = 3 * MIB))

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.Text(listOf(PreviewLine("head")), shownBytes = MIB))
            }

            test("should call an image above the limit too large") {
                // when
                viewModel.preview(file("/a.png", size = 20 * MIB + 1))

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.TooLarge(20 * MIB))
            }

            test("should decode an image file") {
                // given
                api.contents["/a.png"] = png()

                // when
                viewModel.preview(file("/a.png", size = 100))

                // then
                previewed()
                    .shouldBeInstanceOf<Loadable.Ready<PreviewContent>>()
                    .value
                    .shouldBeInstanceOf<PreviewContent.Image>()
                    .bitmap.width shouldBe PNG_SIZE
            }

            test("should say it cannot preview an image that does not decode") {
                // given
                api.contents["/a.png"] = "not an image".encodeToByteArray()

                // when
                viewModel.preview(file("/a.png", size = 12))

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.Unreadable)
            }

            test("should show the error when the download fails") {
                // given
                api.failures["download"] = ApiException(ApiStatus.OFFLINE)

                // when
                viewModel.preview(file("/a.txt"))

                // then
                previewed() shouldBe Loadable.Failed(ApiStatus.OFFLINE.fallback)
            }

            test("should show loading until the file arrives") {
                // given
                api.downloadGate = CompletableDeferred()
                api.contents["/a.txt"] = "hello".encodeToByteArray()
                viewModel.preview(file("/a.txt", size = 5))
                viewModel.state.value.preview shouldBe FilePreview(file("/a.txt", size = 5), Loadable.Loading)

                // when
                api.downloadGate?.complete(Unit)

                // then
                previewed() shouldBe Loadable.Ready(PreviewContent.Text(listOf(PreviewLine("hello")), shownBytes = null))
            }

            test("should cancel the download when the preview closes") {
                // given
                api.downloadGate = CompletableDeferred()
                viewModel.preview(file("/a.txt"))

                // when
                viewModel.closePreview()

                // then
                api.cancelledDownloads shouldBe listOf("/a.txt")
                viewModel.state.value.preview shouldBe null
            }

            test("should do nothing more when the preview closes twice") {
                // given
                api.downloadGate = CompletableDeferred()
                viewModel.preview(file("/a.txt"))
                viewModel.closePreview()

                // when
                viewModel.closePreview()

                // then
                api.cancelledDownloads shouldBe listOf("/a.txt")
                viewModel.state.value.preview shouldBe null
            }

            test("should not open a preview for a file it cannot show") {
                // when
                viewModel.preview(file("/a.zip"))

                // then
                viewModel.state.value.preview shouldBe null
                api.calls.filter { it.startsWith("download") } shouldBe emptyList()
            }
        }

        context("actions") {
            test("should create the folder in the current folder and reload") {
                // given
                viewModel.open("/Docs")
                api.calls.clear()

                // when
                viewModel.createFolder("Notes")

                // then
                api.calls shouldContain "createFolder /Docs/Notes"
                api.calls shouldContain "list /Docs null"
            }

            test("should rename the entry") {
                // when
                viewModel.rename(file("/a.txt"), "c.txt")

                // then
                api.calls shouldContain "rename /a.txt c.txt"
            }

            test("should show the server message when an action fails") {
                // given
                api.failures["createFolder"] = ApiException(ApiStatus.CONFLICT, "Directory Docs exists")

                // when
                viewModel.createFolder("Docs")

                // then
                viewModel.snackbar.first().message shouldBe "Directory Docs exists"
                viewModel.state.value.working shouldBe 0
            }
        }
    })
