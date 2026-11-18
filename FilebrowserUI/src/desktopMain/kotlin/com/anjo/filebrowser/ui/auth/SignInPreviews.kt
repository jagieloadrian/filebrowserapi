package com.anjo.filebrowser.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.anjo.filebrowser.ui.PHONE_HEIGHT_DP
import com.anjo.filebrowser.ui.PHONE_WIDTH_DP
import com.anjo.filebrowser.ui.PreviewTheme

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun SignInPreview() = PreviewTheme { SignInScreen(error = null, onSignIn = {}) }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun SignInErrorPreview() = PreviewTheme { SignInScreen(error = SESSION_ENDED, onSignIn = {}) }
