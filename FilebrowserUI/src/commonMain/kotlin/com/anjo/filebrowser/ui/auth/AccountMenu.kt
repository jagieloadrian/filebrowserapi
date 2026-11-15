package com.anjo.filebrowser.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anjo.filebrowser.ui.theme.Spacing
import org.koin.compose.koinInject

private const val ACCOUNT = "Account"

@Composable
fun AccountMenu(session: AuthSession = koinInject()) {
    val auth by session.state.collectAsState()
    val signedIn = auth as? AuthState.SignedIn ?: return
    val name = signedIn.user ?: ACCOUNT
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = "Account menu for $name")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                "Signed in as $name",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            )
            DropdownMenuItem(
                text = { Text("Sign out") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                onClick = {
                    open = false
                    session.signOut()
                },
            )
        }
    }
}
