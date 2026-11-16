package com.anjo.filebrowser.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anjo.filebrowser.ui.api.ApiException
import com.anjo.filebrowser.ui.api.ApiStatus
import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.Snack
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.FilePreview
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.PreviewContent
import com.anjo.filebrowser.ui.platform.pickFile
import com.anjo.filebrowser.ui.platform.saveFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

internal const val SEARCH_DELAY_MS = 300L
private const val UNDO = "Undo"
private const val REPLACE = "Replace"

@KoinViewModel
class FilesViewModel(
    private val api: FilebrowserApi,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FilesUiState())
    val state: StateFlow<FilesUiState> = mutableState.asStateFlow()
    private val snacks = Channel<Snack>(Channel.BUFFERED)
    val snackbar: Flow<Snack> = snacks.receiveAsFlow()
    private var loading: Job? = null
    private var previewing: Job? = null

    fun refresh() = load()

    fun open(path: String) {
        mutableState.update { it.copy(path = path, query = "", entries = Loadable.Loading) }
        load()
    }

    fun search(query: String) {
        mutableState.update { it.copy(query = query) }
        load(SEARCH_DELAY_MS)
    }

    fun uploadFromDevice() = act { pickFile()?.let { send(it, overwrite = false) } }

    fun upload(
        file: PickedFile,
        overwrite: Boolean = false,
    ) = act { send(file, overwrite) }

    fun download(entry: Entry) = act { saveFile(entry.name) { api.download(entry) } }

    fun preview(entry: Entry) {
        val kind = previewKind(entry) ?: return
        previewing?.cancel()
        mutableState.update { it.copy(preview = FilePreview(entry)) }
        previewing =
            viewModelScope.launch {
                val content = previewContent(entry, kind)
                mutableState.update { it.copy(preview = it.preview?.copy(content = content)) }
            }
    }

    fun closePreview() {
        previewing?.cancel()
        mutableState.update { it.copy(preview = null) }
    }

    private suspend fun previewContent(
        entry: Entry,
        kind: PreviewKind,
    ): Loadable<PreviewContent> {
        val oversized = entry.size > kind.maxBytes
        if (oversized && !kind.partial) return Loadable.Ready(PreviewContent.TooLarge(kind.maxBytes))
        val shownBytes = kind.maxBytes.takeIf { oversized }
        return try {
            val bytes = api.download(entry, shownBytes)
            Loadable.Ready(
                withContext(Dispatchers.Default) {
                    when (kind) {
                        PreviewKind.TEXT -> {
                            val text = bytes.decodeToString(0, minOf(bytes.size.toLong(), kind.maxBytes).toInt())
                            PreviewContent.Text(previewLines(text), shownBytes)
                        }

                        PreviewKind.IMAGE -> {
                            previewImage(bytes)
                        }
                    }
                },
            )
        } catch (e: ApiException) {
            Loadable.Failed(e.message)
        }
    }

    fun createFolder(name: String) = act { api.createFolder(state.value.path, name) }

    fun rename(
        entry: Entry,
        newName: String,
    ) = act { api.rename(entry, newName) }

    fun delete(entry: Entry): Unit =
        act {
            api.delete(entry)
            snacks.send(Snack("Deleted ${entry.name}", UNDO) { restore(entry) })
        }

    private fun restore(entry: Entry): Unit =
        act {
            val item =
                api.trash().filter { it.path == entry.path }.maxByOrNull { it.deletedAt }
                    ?: throw ApiException(ApiStatus.NOT_FOUND)
            api.restore(item)
            snacks.send(Snack("Restored ${entry.name}"))
        }

    private suspend fun send(
        file: PickedFile,
        overwrite: Boolean,
    ) {
        try {
            api.upload(state.value.path, file, overwrite)
            snacks.send(Snack("Uploaded ${file.name}"))
        } catch (e: ApiException) {
            if (e.status != ApiStatus.CONFLICT || overwrite) throw e
            snacks.send(Snack(e.message, REPLACE) { upload(file, overwrite = true) })
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(working = it.working + 1) }
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snacks.send(Snack(e.message ?: ApiStatus.FAILED.fallback))
            } finally {
                mutableState.update { it.copy(working = it.working - 1) }
            }
            load()
        }
    }

    private fun load(delayMillis: Long = 0) {
        loading?.cancel()
        loading =
            viewModelScope.launch {
                delay(delayMillis)
                val path = state.value.path
                val query = state.value.query.trim()
                try {
                    val (entries, quota) =
                        coroutineScope {
                            val quota = async { api.quota() }
                            api.listEntries(path, query.ifEmpty { null }) to quota.await()
                        }
                    val shown = if (query.isEmpty()) entries.filter { it.parent == path } else entries
                    val sorted = shown.sortedWith(compareBy<Entry>({ !it.directory }, { it.name.lowercase() }))
                    mutableState.update { it.copy(entries = Loadable.Ready(sorted), quota = quota) }
                } catch (e: ApiException) {
                    mutableState.update { it.copy(entries = Loadable.Failed(e.message)) }
                }
            }
    }
}
