package com.anjo.filebrowser.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.AuthState
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.Spacing
import com.anjo.filebrowser.ui.theme.ThemeMode
import org.koin.compose.koinInject

private const val ACCOUNT = "Account"

private val ThemeMode.icon: ImageVector
    get() =
        when (this) {
            ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
            ThemeMode.LIGHT -> Icons.Outlined.LightMode
            ThemeMode.DARK -> Icons.Outlined.DarkMode
        }

@Composable
fun AppMenu(
    session: AuthSession = koinInject(),
    settings: AppearanceSettings = koinInject(),
) {
    val auth by session.state.collectAsState()
    val appearance by settings.state.collectAsState()
    val user = (auth as? AuthState.SignedIn)?.let { it.user ?: ACCOUNT }
    var open by remember { mutableStateOf(false) }
    Box {
        val label = if (user == null) "Settings" else "Account menu for $user"
        Tooltip(label) {
            IconButton(onClick = { open = true }) {
                Icon(if (user == null) Icons.Outlined.Settings else Icons.Outlined.AccountCircle, contentDescription = label)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            user?.let { MenuHeading("Signed in as $it") }
            MenuHeading("Color theme")
            Palette.entries.forEach { palette ->
                Choice(palette.label, palette == appearance.palette, { settings.choose(palette) }) { Swatch(Color(palette.seed)) }
            }
            HorizontalDivider()
            MenuHeading("Light or dark")
            ThemeMode.entries.forEach { mode ->
                Choice(mode.label, mode == appearance.mode, { settings.choose(mode) }) { Icon(mode.icon, contentDescription = null) }
            }
            if (user != null) {
                HorizontalDivider()
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
}

@Composable
private fun MenuHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).semantics { heading() },
    )
}

@Composable
private fun Choice(
    label: String,
    chosen: Boolean,
    onChoose: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = leading,
        trailingIcon = if (chosen) ({ Icon(Icons.Outlined.Check, contentDescription = null) }) else null,
        onClick = onChoose,
        modifier = Modifier.semantics { selected = chosen },
    )
}

@Composable
private fun Swatch(color: Color) {
    Box(
        Modifier
            .size(Spacing.l)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}
