package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.theme.Spacing

private const val SKELETON_ROWS = 6

@Composable
fun SkeletonList() {
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "Loading" }) {
        repeat(SKELETON_ROWS) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Placeholder(Modifier.size(Spacing.xl).clip(CircleShape))
                Spacer(Modifier.size(Spacing.m))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Placeholder(Modifier.fillMaxWidth(0.6f).height(Spacing.m).clip(RoundedCornerShape(Spacing.xs)))
                    Placeholder(Modifier.fillMaxWidth(0.4f).height(Spacing.s).clip(RoundedCornerShape(Spacing.xs)))
                }
            }
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Spacer(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Message(icon = icon, tint = MaterialTheme.colorScheme.onSurfaceVariant, title = title, body = body)
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Message(icon = Icons.Outlined.ErrorOutline, tint = MaterialTheme.colorScheme.error, title = "Could not load", body = message) {
        FilledTonalButton(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun Message(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    action: @Composable () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Spacing.s))
        action()
    }
}
