package com.example.memeapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.memeapp.di.initKoin

fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "MemeApp",
    ) {
        App()
    }
}