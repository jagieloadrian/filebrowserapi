package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.AuthState
import com.anjo.filebrowser.ui.auth.SignInScreen
import com.anjo.filebrowser.ui.auth.SigningIn
import com.anjo.filebrowser.ui.common.label
import com.anjo.filebrowser.ui.files.FilesScreen
import com.anjo.filebrowser.ui.theme.FilebrowserTheme
import com.anjo.filebrowser.ui.theme.Spacing
import com.anjo.filebrowser.ui.trash.TrashScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import org.koin.plugin.module.dsl.module

const val APP_NAME = "Filebrowser"

internal enum class Destination(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    FILES(Icons.Outlined.Folder, Icons.Filled.Folder),
    TRASH(Icons.Outlined.Delete, Icons.Filled.Delete),
}

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration { module<AppModule>() }) {
        FilebrowserTheme {
            Surface(Modifier.fillMaxSize()) {
                val session = koinInject<AuthSession>()
                when (val auth = session.state.collectAsState().value) {
                    AuthState.Off, is AuthState.SignedIn -> Browser()
                    AuthState.Working -> SigningIn(session::cancel)
                    is AuthState.SignedOut -> SignInScreen(auth.error, session::signIn)
                }
            }
        }
    }
}

@Composable
internal fun Browser() {
    var destination by remember { mutableStateOf(Destination.FILES) }
    BrowserShell(destination, onNavigate = { destination = it }) { tabs ->
        when (destination) {
            Destination.FILES -> FilesScreen(tabs)
            Destination.TRASH -> TrashScreen(tabs)
        }
    }
}

@Composable
internal fun BrowserShell(
    destination: Destination,
    onNavigate: (Destination) -> Unit,
    content: @Composable (tabs: @Composable () -> Unit) -> Unit,
) = BoxWithConstraints {
    val rail = maxWidth >= Spacing.expanded
    Row {
        if (rail) DestinationRail(destination, onNavigate)
        content { if (!rail) DestinationTabs(destination, onNavigate) }
    }
}

@Composable
private fun DestinationTabs(
    destination: Destination,
    onNavigate: (Destination) -> Unit,
) {
    PrimaryTabRow(selectedTabIndex = destination.ordinal) {
        Destination.entries.forEach {
            Tab(
                selected = it == destination,
                onClick = { onNavigate(it) },
                text = { Text(it.label) },
                icon = { DestinationIcon(it, it == destination) },
            )
        }
    }
}

@Composable
private fun DestinationRail(
    destination: Destination,
    onNavigate: (Destination) -> Unit,
) {
    NavigationRail {
        Destination.entries.forEach {
            NavigationRailItem(
                selected = it == destination,
                onClick = { onNavigate(it) },
                icon = { DestinationIcon(it, it == destination) },
                label = { Text(it.label) },
            )
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: Destination,
    selected: Boolean,
) = Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null)
