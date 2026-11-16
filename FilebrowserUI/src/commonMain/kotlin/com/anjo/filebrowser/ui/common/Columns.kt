package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anjo.filebrowser.ui.theme.Spacing

val FOLDER_COLUMN = 200.sp
val TYPE_COLUMN = 112.sp
val SIZE_COLUMN = 96.sp
val TIME_COLUMN = 232.sp
const val ROW_DIVIDER = "row-divider"
const val COLUMN_DIVIDER = "column-divider"
private val ROW_MIN_HEIGHT = 56.dp

@Composable
fun BoxWithConstraintsScope.fitsColumns(): Boolean = maxWidth >= Spacing.expanded * LocalDensity.current.fontScale

fun <T : Any> LazyListScope.dividedItems(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit,
) = itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
    Column {
        if (index > 0) {
            HorizontalDivider(
                Modifier.padding(horizontal = Spacing.m).testTag(ROW_DIVIDER),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        content(item)
    }
}

@Composable
fun ColumnsRow(
    name: String,
    columns: List<Pair<String, TextUnit>>,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .height(IntrinsicSize.Min)
            .padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
            leading()
            Text(name, Modifier.weight(1f), color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            columns.forEach { (text, width) ->
                VerticalDivider(Modifier.testTag(COLUMN_DIVIDER), color = colors.outlineVariant)
                Text(
                    text,
                    Modifier.width(with(density) { width.toDp() }),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}
