package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
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
import com.anjo.filebrowser.ui.trash.TrashScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import org.koin.plugin.module.dsl.module

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
    val tabs = @Composable { DestinationTabs(destination, onNavigate = { destination = it }) }
    when (destination) {
        Destination.FILES -> FilesScreen(tabs)
        Destination.TRASH -> TrashScreen(tabs)
    }
}

@Composable
internal fun DestinationTabs(
    destination: Destination,
    onNavigate: (Destination) -> Unit,
) {
    PrimaryTabRow(selectedTabIndex = destination.ordinal) {
        Destination.entries.forEach {
            Tab(
                selected = it == destination,
                onClick = { onNavigate(it) },
                text = { Text(it.label) },
                icon = { Icon(if (it == destination) it.selectedIcon else it.icon, contentDescription = null) },
            )
        }
    }
}
