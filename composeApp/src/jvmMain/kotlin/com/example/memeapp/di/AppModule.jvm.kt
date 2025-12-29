package com.example.memeapp.di

import com.example.memeapp.meme_editor.data.CacheStorageStrategy
import com.example.memeapp.meme_editor.data.PlatformMemeExporter
import com.example.memeapp.meme_editor.domain.MemeExporter
import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import com.example.memeapp.meme_editor.presentation.util.PlatformShareSheet
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformAppModule= module {
    factoryOf(::PlatformMemeExporter) bind MemeExporter::class
    factoryOf(::CacheStorageStrategy) bind SaveToStorageStrategy::class
    factoryOf(::PlatformShareSheet)
}