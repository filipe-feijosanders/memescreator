package com.example.memeapp.meme_editor.domain

interface SaveToStorageStrategy {
    fun getFilePath(fileName: String): String
}