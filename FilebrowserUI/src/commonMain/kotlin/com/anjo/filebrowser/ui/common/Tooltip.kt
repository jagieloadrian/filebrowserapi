package com.anjo.filebrowser.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    text: String,
    enabled: Boolean = true,
    initiallyVisible: Boolean = false,
    content: @Composable () -> Unit,
) = TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(text) } },
    state = rememberTooltipState(initialIsVisible = initiallyVisible, isPersistent = initiallyVisible),
    enableUserInput = enabled,
    content = content,
)
