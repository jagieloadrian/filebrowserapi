package com.anjo.filebrowser.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.anjo.filebrowser.ui.PHONE_HEIGHT_DP
import com.anjo.filebrowser.ui.PHONE_WIDTH_DP
import com.anjo.filebrowser.ui.PreviewTheme
import com.anjo.filebrowser.ui.theme.ThemeMode

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun SignInPreview() = PreviewTheme { SignInScreen(error = null, onSignIn = {}) }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun SignInErrorPreview() = PreviewTheme { SignInScreen(error = SESSION_ENDED, onSignIn = {}) }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun SignInDarkPreview() = PreviewTheme(mode = ThemeMode.DARK) { SignInScreen(error = null, onSignIn = {}) }
