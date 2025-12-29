package com.example.memeapp.meme_editor.data

import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy

actual class CacheStorageStrategy : SaveToStorageStrategy {
    actual override fun getFilePath(fileName: String): String {
       return ""
    }
}