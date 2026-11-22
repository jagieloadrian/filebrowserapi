package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.APP_NAME

private val ACTION_ICONS_MIN_WIDTH = 252.dp

class BarAction(
    val label: String,
    val icon: ImageVector,
    val color: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    actions: List<BarAction>,
    leading: @Composable () -> Unit = {},
    initialMenuExpanded: Boolean = false,
) = BoxWithConstraints {
    val icons = maxWidth >= ACTION_ICONS_MIN_WIDTH * LocalDensity.current.fontScale
    TopAppBar(
        title = { Text(APP_NAME, Modifier.semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            leading()
            if (icons) actions.forEach { ActionIcon(it) }
            AppMenu(actions = if (icons) emptyList() else actions, initiallyExpanded = initialMenuExpanded)
        },
    )
}

@Composable
private fun ActionIcon(action: BarAction) =
    Tooltip(action.label) {
        IconButton(onClick = action.onClick) {
            Icon(action.icon, contentDescription = action.label, tint = action.color.takeOrElse { LocalContentColor.current })
        }
    }
