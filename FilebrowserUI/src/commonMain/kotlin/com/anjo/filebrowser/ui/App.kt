package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.anjo.filebrowser.ui.files.FilesScreen
import com.anjo.filebrowser.ui.theme.FilebrowserTheme
import com.anjo.filebrowser.ui.theme.Spacing
import com.anjo.filebrowser.ui.trash.TrashScreen
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.plugin.module.dsl.module

private enum class Destination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    FILES("Files", Icons.Outlined.Folder, Icons.Filled.Folder),
    TRASH("Trash", Icons.Outlined.Delete, Icons.Filled.Delete),
}

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration { module<AppModule>() }) {
        FilebrowserTheme {
            Surface(Modifier.fillMaxSize()) {
                var destination by remember { mutableStateOf(Destination.FILES) }
                BoxWithConstraints {
                    if (maxWidth >= Spacing.wide) {
                        Row {
                            NavigationRail {
                                Destination.entries.forEach {
                                    NavigationRailItem(
                                        selected = it == destination,
                                        onClick = { destination = it },
                                        icon = { Icon(if (it == destination) it.selectedIcon else it.icon, contentDescription = null) },
                                        label = { Text(it.label) },
                                    )
                                }
                            }
                            Screen(destination)
                        }
                    } else {
                        Column {
                            Column(Modifier.weight(1f)) { Screen(destination) }
                            NavigationBar {
                                Destination.entries.forEach {
                                    NavigationBarItem(
                                        selected = it == destination,
                                        onClick = { destination = it },
                                        icon = { Icon(if (it == destination) it.selectedIcon else it.icon, contentDescription = null) },
                                        label = { Text(it.label) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen(destination: Destination) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = Spacing.content)) {
            when (destination) {
                Destination.FILES -> FilesScreen()
                Destination.TRASH -> TrashScreen()
            }
        }
    }
}
