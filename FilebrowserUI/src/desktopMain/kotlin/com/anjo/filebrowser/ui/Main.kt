package com.anjo.filebrowser.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

private const val MIN_WIDTH = 320
private const val MIN_HEIGHT = 480

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = APP_NAME) {
            LaunchedEffect(window) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
            App()
        }
    }
