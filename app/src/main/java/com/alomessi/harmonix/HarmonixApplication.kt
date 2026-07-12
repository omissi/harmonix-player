package com.alomessi.harmonix

import android.app.Application
import com.alomessi.harmonix.player.AudioEffectsManager

class HarmonixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AudioEffectsManager.initialize(this)
    }
}
