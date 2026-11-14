package com.anjo.filebrowser.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun SnackbarEffect(
    snacks: Flow<Snack>,
    host: SnackbarHostState,
) {
    LaunchedEffect(snacks) {
        snacks.collect { snack ->
            val result =
                host.showSnackbar(
                    message = snack.message,
                    actionLabel = snack.action,
                    withDismissAction = snack.action != null,
                    duration = if (snack.action != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) snack.onAction()
        }
    }
}
