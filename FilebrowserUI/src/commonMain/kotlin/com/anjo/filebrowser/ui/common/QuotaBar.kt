package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.theme.Spacing

@Composable
fun QuotaBar(quota: Quota?) {
    if (quota == null) return
    val fraction = if (quota.limit > 0) (quota.used.toFloat() / quota.limit).coerceIn(0f, 1f) else 0f
    Column(
        Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).widthIn(max = Spacing.content).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            "${formatSize(quota.used)} of ${formatSize(quota.limit)} used",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    }
}
