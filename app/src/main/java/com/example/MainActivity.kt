package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.player.PlaybackStateHolder
import com.example.ui.ShujalMusicRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val openNowPlaying = intent?.getBooleanExtra("open_now_playing", false) == true
        setContent {
            ShujalMusicRoot(openNowPlayingOnLaunch = openNowPlaying)
        }
    }

    override fun onStart() {
        super.onStart()
        PlaybackStateHolder.setAppForeground(true)
    }

    override fun onStop() {
        super.onStop()
        PlaybackStateHolder.setAppForeground(false)
    }
}
