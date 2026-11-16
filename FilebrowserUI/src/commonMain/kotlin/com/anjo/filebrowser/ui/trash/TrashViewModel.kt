package com.anjo.filebrowser.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anjo.filebrowser.ui.api.ApiException
import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.Snack
import com.anjo.filebrowser.ui.model.TrashItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class TrashViewModel(
    private val api: FilebrowserApi,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = mutableState.asStateFlow()
    private val snacks = Channel<Snack>(Channel.BUFFERED)
    val snackbar: Flow<Snack> = snacks.receiveAsFlow()
    private var loading: Job? = null

    fun restore(item: TrashItem) =
        act {
            api.restore(item)
            snacks.send(Snack("Restored ${item.name}"))
        }

    fun deleteForever(item: TrashItem) =
        act {
            api.deleteForever(item)
            snacks.send(Snack("Deleted ${item.name} forever"))
        }

    fun emptyTrash() =
        act {
            api.emptyTrash()
            snacks.send(Snack("Trash emptied"))
        }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(working = it.working + 1) }
            try {
                block()
            } catch (e: ApiException) {
                snacks.send(Snack(e.message))
            } finally {
                mutableState.update { it.copy(working = it.working - 1) }
            }
            refresh()
        }
    }

    fun refresh() {
        loading?.cancel()
        loading =
            viewModelScope.launch {
                try {
                    val (items, quota) =
                        coroutineScope {
                            val quota = async { api.quota() }
                            api.trash() to quota.await()
                        }
                    mutableState.update { it.copy(items = Loadable.Ready(items.sortedByDescending { it.deletedAt }), quota = quota) }
                } catch (e: ApiException) {
                    mutableState.update { it.copy(items = Loadable.Failed(e.message)) }
                }
            }
    }
}
