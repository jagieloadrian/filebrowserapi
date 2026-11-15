package com.anjo.filebrowser.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.theme.Spacing
import org.koin.compose.koinInject

private const val ACCOUNT = "Account"
private val NAME_WIDTH = 120.dp

@Composable
fun AccountMenu(session: AuthSession = koinInject()) {
    val auth by session.state.collectAsState()
    val signedIn = auth as? AuthState.SignedIn ?: return
    val name = signedIn.user ?: ACCOUNT
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = "Account menu for $name" }) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = null)
            Spacer(Modifier.size(Spacing.xs))
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = NAME_WIDTH))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
