package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.theme.Spacing

val FOLDER_COLUMN = 200.dp
val TYPE_COLUMN = 112.dp
val SIZE_COLUMN = 96.dp
val TIME_COLUMN = 232.dp

@Composable
fun NameWithColumns(
    name: String,
    columns: List<Pair<String, Dp>>,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        columns.forEach { (text, width) ->
            Text(
                text,
                Modifier.padding(start = Spacing.m).width(width),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
