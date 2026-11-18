package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
val TIME_COLUMN = 168.sp
const val ROW_DIVIDER = "row-divider"
const val COLUMN_DIVIDER = "column-divider"
private val ROW_MIN_HEIGHT = 56.dp
private val ICON_SIZE = 24.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileName(
    name: String,
    maxLines: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    var truncated by remember { mutableStateOf(false) }
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(name) } },
            state = rememberTooltipState(),
            enableUserInput = truncated,
        ) {
            Text(
                name,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { truncated = it.hasVisualOverflow },
            )
        }
    }
}

@Composable
fun ColumnsHeader(columns: List<Pair<String, TextUnit>>) =
    Column {
        ColumnsRow(
            "Name",
            columns,
            leading = { Spacer(Modifier.size(ICON_SIZE)) },
            trailing = { Spacer(Modifier.minimumInteractiveComponentSize()) },
            header = true,
        )
        HorizontalDivider(Modifier.padding(horizontal = Spacing.m), color = MaterialTheme.colorScheme.outlineVariant)
    }

@Composable
fun ColumnsRow(
    name: String,
    columns: List<Pair<String, TextUnit>>,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: Boolean = false,
) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
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
            if (header) {
                Text(name, Modifier.weight(1f), style = style)
            } else {
                FileName(name, maxLines = 1, Modifier.weight(1f), colors.onSurface)
            }
            columns.forEach { (text, width) ->
                VerticalDivider(Modifier.testTag(COLUMN_DIVIDER), color = colors.outlineVariant)
                Text(
                    text,
                    Modifier.width(with(density) { width.toDp() }),
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}
