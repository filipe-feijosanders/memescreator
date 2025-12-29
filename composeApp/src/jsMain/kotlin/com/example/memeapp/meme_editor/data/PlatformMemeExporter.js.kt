package com.example.memeapp.meme_editor.data

import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.domain.MemeExporter
import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import com.example.memeapp.meme_editor.presentation.MemeText

actual class PlatformMemeExporter : MemeExporter {
    actual override suspend fun exportMeme(
        backgroundImageBytes: ByteArray,
        memeTexts: List<MemeText>,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String
    ): Result<String> {
        return Result.success("")
    }
}