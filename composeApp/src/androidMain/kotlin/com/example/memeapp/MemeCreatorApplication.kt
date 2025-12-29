package com.example.memeapp

import android.app.Application
import com.example.memeapp.di.initKoin
import org.koin.android.ext.koin.androidContext

class MemeCreatorApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MemeCreatorApplication)
        }
    }
}