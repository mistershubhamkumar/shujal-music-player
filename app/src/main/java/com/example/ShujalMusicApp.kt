package com.example

import android.app.Application
import com.example.ads.AdMobManager

class ShujalMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdMobManager.initializeAsync(this)
    }
}
