package com.example.memeapp

import androidx.compose.runtime.*
import com.example.memeapp.core.theme.MemeCreatorTheme
import com.example.memeapp.meme_gallery.presentation.MemeGalleryScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MemeCreatorTheme {
        MemeGalleryScreen {  }
    }
}