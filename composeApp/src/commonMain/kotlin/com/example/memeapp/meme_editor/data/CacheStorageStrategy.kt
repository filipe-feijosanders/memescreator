package com.example.memeapp.meme_editor.data

import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy

expect class CacheStorageStrategy: SaveToStorageStrategy {
    override fun getFilePath(fileName: String): String
}