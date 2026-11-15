package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.anjo.filebrowser.ui.theme.Spacing

val FOLDER_COLUMN = 200.sp
val TYPE_COLUMN = 112.sp
val SIZE_COLUMN = 96.sp
val TIME_COLUMN = 232.sp

@Composable
fun BoxWithConstraintsScope.fitsColumns(): Boolean = maxWidth >= Spacing.expanded * LocalDensity.current.fontScale

@Composable
fun NameWithColumns(
    name: String,
    columns: List<Pair<String, TextUnit>>,
) {
    val density = LocalDensity.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        columns.forEach { (text, width) ->
            Text(
                text,
                Modifier.padding(start = Spacing.m).width(with(density) { width.toDp() }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
