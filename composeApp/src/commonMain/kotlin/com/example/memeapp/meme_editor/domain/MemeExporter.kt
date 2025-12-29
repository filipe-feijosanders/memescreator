@file:OptIn(ExperimentalUuidApi::class)

package com.example.memeapp.meme_editor.domain

import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.presentation.MemeText
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MemeExporter {
    suspend fun exportMeme(
        backgroundImageBytes: ByteArray,
        memeTexts: List<MemeText>,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String = "meme_${Uuid.random()}.jpg"
    ): Result<String>
}